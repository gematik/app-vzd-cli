/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
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
import de.gematik.ti.directory.pki.CertificateDataDER
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bouncycastle.util.encoders.Base64
import kotlin.io.path.inputStream
import kotlin.io.path.name

class CertInfoCommand : CliktCommand(name = "cert-info", help = "Show details of a certificate") {
    private val logger = KotlinLogging.logger {}
    private val files by argument().path(mustBeReadable = true, canBeDir = false).multiple()
    private val outputFormat by option()
        .switch(
            "--yaml" to RepresentationFormat.YAML,
            "--json" to RepresentationFormat.JSON,
        ).default(RepresentationFormat.YAML)
    private val ocspOptions by OcspOptions()
    private val context by requireObject<AdminCliContext>()

    override fun run() =
        catching {
            files.forEach {
                logger.info { "Processing ${it.name}" }
                val certB64 = Base64.toBase64String(it.inputStream().readBytes())
                val userCertificate = CertificateDataDER(certB64)
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
