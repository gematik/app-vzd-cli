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
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.cli.*
import de.gematik.ti.directory.pki.hasAdmission
import de.gematik.ti.directory.pki.loadCertificates
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.io.path.name

class CertInfoCommand : CliktCommand(name = "cert-info", help = "Show details of certificates (DER, PEM or PKCS#12)") {
    private val logger = KotlinLogging.logger {}
    private val files by argument().path(mustBeReadable = true, canBeDir = false).multiple()
    private val outputFormat by option()
        .switch(
            "--yaml" to RepresentationFormat.YAML,
            "--json" to RepresentationFormat.JSON,
        ).default(RepresentationFormat.YAML)
    private val ocspOptions by OcspOptions()
    private val certificateInput by CertificateInputOptions()
    private val context by requireObject<AdminCliContext>()

    override fun run() =
        catching {
            val password = certificateInput.passwordProvider(this, noPrompt = false)
            files.forEach { file ->
                logger.info { "Processing ${file.name}" }
                loadCertificates(file, password).forEach { userCertificate ->
                    if (!userCertificate.certificate.hasAdmission()) {
                        echo("${userCertificate.certificate.subjectX500Principal.name}: no admission statement, not a TI certificate, skipped", err = true)
                        return@forEach
                    }
                    val certificateInfo = userCertificate.certificateInfo

                    if (ocspOptions.enableOcsp) {
                        certificateInfo.ocspResponse =
                            runBlocking {
                                context.adminAPI.globalAPI.pkiClient
                                    .ocsp(userCertificate)
                            }
                    }

                    when (outputFormat) {
                        RepresentationFormat.JSON -> echo(certificateInfo.toJsonPrettyNoDefaults())
                        RepresentationFormat.YAML -> echo(certificateInfo.toYamlNoDefaults())
                        else -> throw UsageError("Cant use output format: $outputFormat")
                    }
                }
            }
        }
}
