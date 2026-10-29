/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.AdminResponseException
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.CertificateEntryDraft
import de.gematik.ti.directory.admin.CreateDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.admin.draftFromCertificates
import de.gematik.ti.directory.admin.explainServerError
import de.gematik.ti.directory.admin.setAttributes
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.toJsonPretty
import de.gematik.ti.directory.cli.toYaml
import de.gematik.ti.directory.cli.toYamlNoDefaults
import de.gematik.ti.directory.pki.loadCertificates
import de.gematik.ti.directory.validation.FindingSeverity
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml
import java.io.File
import java.nio.file.Path

class AddBaseCommand : CliktCommand(name = "add-base", help = "Add new directory entry, optionally derived from an ENC certificate") {
    private val logger = KotlinLogging.logger {}
    private val attrs: Map<String, String> by option(
        "-s",
        "--set",
        metavar = "ATTR=VALUE",
        help = "Set the attribute value in BaseDirectoryEntry.",
    ).associate()
    private val deprecatedFile: String? by option(
        "--file",
        "-f",
        metavar = "FILENAME",
        help = "Read the BaseDirectoryEntry from specified file, use - to read data from STDIN",
    ).deprecated("WARINING: -f / --file ist deprecated. Use argument without option instead.")
    private val inputFile by argument(
        help = "Read the BaseDirectoryEntry from specified file, use - to read data from STDIN",
    ).path(mustExist = true, canBeDir = false, mustBeReadable = true).optional()
    private val fromCert: Path? by option(
        "--from-cert",
        metavar = "FILE",
        help = "Create the entry from an ENC certificate (DER, PEM or PKCS#12) and add the certificate in the same request",
    ).path(mustExist = true, canBeDir = false, mustBeReadable = true)
    private val noPrompt: Boolean by option("--no-prompt", help = "Never ask interactively; fail on missing values").flag()
    private val certificateInput by CertificateInputOptions()
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val ignore by option("--ignore", "-i", help = "Ignore Error 409 (entry exists).").flag()
    private val format by option()
        .switch(
            "--yaml" to RepresentationFormat.YAML,
            "--json" to RepresentationFormat.JSON,
        ).default(RepresentationFormat.YAML)

    override fun run() =
        catching {
            fromCert?.let { file ->
                if (inputFile != null || deprecatedFile != null) {
                    throw UsageError("Use either --from-cert or an input file, not both")
                }
                output(createFromCertificate(file))
                return@catching
            }

            val input = inputFile ?: deprecatedFile

            val data =
                if (input != null && input == "-") {
                    logger.debug { "Loading from STDIN" }
                    generateSequence(::readLine).joinToString("\n")
                } else if (input != null) {
                    logger.debug { "Loading file: $input" }
                    File(input.toString()).readText(Charsets.UTF_8)
                } else {
                    null
                }

            val baseDirectoryEntry: BaseDirectoryEntry =
                data?.let {
                    when (format) {
                        RepresentationFormat.YAML -> Yaml.decodeFromString(it)
                        RepresentationFormat.JSON -> Json.decodeFromString(it)
                        else -> throw CliktError("Unsupported format: $format")
                    }
                } ?: run {
                    val telematikID = attrs["telematikID"] ?: throw UsageError("Option --set telematikID=<VALUE> or --file is required")
                    val entryType = attrs["entryType"]?.toInt() ?: throw UsageError("Option --set entryType=<VALUE> or --file is required")
                    BaseDirectoryEntry(
                        telematikID = telematikID,
                        entryType = listOf(entryType.toString()),
                    )
                }

            setAttributes(baseDirectoryEntry, attrs)

            logger.debug { "Creating new directory entry with telematikID: ${baseDirectoryEntry.telematikID}" }

            val result =
                try {
                    val dn = runBlocking { context.client.addDirectoryEntry(CreateDirectoryEntry(baseDirectoryEntry)) }
                    logger.info("Created new DirectoryEntry: ${dn.uid}")

                    val query = mapOf("uid" to dn.uid)
                    runBlocking { context.client.readDirectoryEntry(query) }
                } catch (e: AdminResponseException) {
                    if (!ignore || e.response.status != HttpStatusCode.Conflict) {
                        throw e
                    }
                    logger.warn { "Entry with telematikID=${baseDirectoryEntry.telematikID} already exists. Ignoring conflict." }
                    runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to baseDirectoryEntry.telematikID)) }
                }

            output(result)
        }

    private fun output(result: List<DirectoryEntry>?) {
        when (format) {
            RepresentationFormat.JSON -> echo(result?.first()?.directoryEntryBase?.toJsonPretty())
            RepresentationFormat.YAML -> echo(result?.first()?.directoryEntryBase?.toYaml())
            else -> throw UsageError("Cant load for editing in for format: $format")
        }
    }

    private fun createFromCertificate(file: Path): List<DirectoryEntry>? {
        val certificates = loadCertificates(file, certificateInput.passwordProvider(this, noPrompt))
        var draft = draftFromCertificates(certificates, attrs)
        draft.rejected.forEach { echo("Warning: ignoring certificate $it", err = true) }

        val interactive = !noPrompt && System.console() != null
        if (interactive) {
            draft = promptForErrors(draft) { finding, default ->
                TermUi.prompt(promptText(finding), default = default, showDefault = true, console = currentContext.console)
            }
        }
        if (draft.hasErrors()) {
            throw CliktError(formatFindingsForAbort(draft, interactive))
        }
        draft.findings
            .filter { it.severity == FindingSeverity.WARNING }
            .forEach { echo("Warning: ${it.attribute}: ${it.message}", err = true) }
        echo("Creating entry from ${draft.certificates.size} certificate(s):\n${draft.base.toYamlNoDefaults()}", err = true)

        val telematikID = draft.base.telematikID
        val professionOids = draft.certificates.last().certificateInfo.admissionStatement.professionOids
        return try {
            val dn = runBlocking { context.client.addDirectoryEntry(draft.toCreateDirectoryEntry()) }
            logger.info("Created new DirectoryEntry: ${dn.uid}")
            runBlocking { context.client.readDirectoryEntry(mapOf("uid" to dn.uid)) }
        } catch (e: AdminResponseException) {
            when {
                e.response.status == HttpStatusCode.Conflict && ignore -> addCertificatesToExistingEntry(draft)
                e.response.status == HttpStatusCode.Unauthorized -> throw e
                else -> {
                    val details = e.details
                    val hint = explainServerError(details, telematikID, professionOids)
                    throw CliktError(if (hint != null) "$details\n$hint" else details)
                }
            }
        }
    }

    private fun addCertificatesToExistingEntry(draft: CertificateEntryDraft): List<DirectoryEntry>? {
        val telematikID = draft.base.telematikID
        echo("Entry with telematikID=$telematikID already exists. Adding certificates to the existing entry.", err = true)
        val existing =
            runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to telematikID)) }?.firstOrNull()
                ?: throw CliktError("Entry with telematikID $telematikID exists but could not be read.")
        val uid = existing.directoryEntryBase.dn?.uid!!
        draft.certificates.forEach { cert ->
            try {
                runBlocking { context.client.addDirectoryEntryCertificate(uid, UserCertificate(userCertificate = cert, telematikID = telematikID)) }
                echo("Added certificate: telematikID=$telematikID serialNumber=${cert.certificateInfo.serialNumber}")
            } catch (e: AdminResponseException) {
                if (e.response.status != HttpStatusCode.Conflict) throw e
                echo("Certificate with serialNumber=${cert.certificateInfo.serialNumber} already exists. Ignoring conflict.", err = true)
            }
        }
        return runBlocking { context.client.readDirectoryEntry(mapOf("uid" to uid)) }
    }
}
