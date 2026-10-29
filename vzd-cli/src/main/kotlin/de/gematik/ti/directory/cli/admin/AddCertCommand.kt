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
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.AdminResponseException
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.CertificateSelectionException
import de.gematik.ti.directory.admin.EntryTypeMapping
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.admin.explainServerError
import de.gematik.ti.directory.admin.selectEncryptionCertificates
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.pki.CertificateDataDER
import de.gematik.ti.directory.pki.loadCertificates
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.mamoe.yamlkt.Yaml

/**
 * Local version of the checks the VZD performs on add_Directory_Entry_Certificate, with an explanation and a fix.
 */
fun checkCertificateAgainstEntry(
    cert: CertificateDataDER,
    entry: BaseDirectoryEntry,
    env: String = "<env>",
): String? {
    val admission = cert.certificateInfo.admissionStatement
    if (admission.registrationNumber != entry.telematikID) {
        return "Certificate telematikID ${admission.registrationNumber} does not match entry ${entry.telematikID}"
    }
    val certEntryType = EntryTypeMapping.entryTypeOf(admission.professionOids) ?: return null
    val entryTypes = entry.entryType.orEmpty().filterNotNull().filter { it.isNotBlank() }
    if (entryTypes.isNotEmpty() && certEntryType.toString() !in entryTypes) {
        return "Certificate yields entryType $certEntryType (${admission.professionOids.joinToString(", ") { EntryTypeMapping.describe(it) }}), " +
            "entry has entryType ${entryTypes.joinToString(", ")}. " +
            "Fix with: vzd-cli admin $env modify-base-attr -t ${entry.telematikID} -s entryType=$certEntryType while the entry has no certificates."
    }
    return null
}

class AddCertCommand : CliktCommand(name = "add-cert", help = "Add certificates (DER, PEM or PKCS#12) to existing DirectoryEntry") {
    private val logger = KotlinLogging.logger {}

    private val files by argument().path(mustBeReadable = true).multiple()
    private val ignore by option("--ignore", "-i", help = "Ignore Error 409 (certificate exists).").flag()
    private val certificateInput by CertificateInputOptions()

    private val context by requireObject<AdminCliEnvironmentContext>()

    override fun run() =
        catching {
            val password = certificateInput.passwordProvider(this, noPrompt = false)
            files.forEach { file ->
                val certificates =
                    try {
                        selectEncryptionCertificates(loadCertificates(file, password))
                    } catch (e: CertificateSelectionException) {
                        throw CliktError("${file.fileName}: ${e.message}")
                    }
                val telematikID = certificates.first().certificateInfo.admissionStatement.registrationNumber

                val entry =
                    runBlocking { context.client.readDirectoryEntry(mapOf("telematikID" to telematikID)) }?.firstOrNull()
                        ?: throw CliktError("Entry with telematikID $telematikID not found.")
                val base = entry.directoryEntryBase
                logger.info { "Found matching Entry: ${base.dn?.uid} ${base.displayName}" }

                certificates.forEach { certDER ->
                    checkCertificateAgainstEntry(certDER, base, context.env.name)?.let { throw CliktError(it) }
                    val info = certDER.certificateInfo
                    val userCertificate = UserCertificate(userCertificate = certDER, telematikID = telematikID)
                    logger.info { "Adding Certificate ${Yaml.encodeToString(info)}" }
                    try {
                        runBlocking { context.client.addDirectoryEntryCertificate(base.dn?.uid!!, userCertificate) }
                        echo("Added certificate: telematikID=$telematikID serialNumber=${info.serialNumber}")
                    } catch (e: AdminResponseException) {
                        when {
                            e.response.status == HttpStatusCode.Conflict && ignore ->
                                logger.warn { "Certificate with serialNumber=${info.serialNumber} already exists. Ignoring conflict." }
                            e.response.status == HttpStatusCode.Unauthorized -> throw e
                            else -> {
                                val details = e.details
                                val hint = explainServerError(details, telematikID, info.admissionStatement.professionOids)
                                throw CliktError(if (hint != null) "$details\n$hint" else details)
                            }
                        }
                    }
                }
            }
        }
}
