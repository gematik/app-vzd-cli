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
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.toJsonPretty
import de.gematik.ti.directory.cli.toYaml
import de.gematik.ti.directory.pki.CertificateDataDER

class TemplateCommand :
    CliktCommand(
        name = "template",
        help = """Create template for a resource
     
     Supported types: base, entry, cert
""",
    ) {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val outputFormat by option()
        .switch(
            "--json" to RepresentationFormat.JSON,
            "--yaml" to RepresentationFormat.YAML,
        ).default(RepresentationFormat.YAML)
    private val resourceType by argument(help = "Specify type of a resource").choice("base", "entry", "cert")

    override fun run() =
        catching {
            val base =
                BaseDirectoryEntry(
                    telematikID = "1-x.1234567890",
                    entryType = listOf("1"),
                    cn = "Name, Vorname",
                    givenName = "Vorname",
                    sn = "Nachname",
                    displayName = "Name, Vorname",
                    streetAddress = "Hauptstr. 1",
                    postalCode = "12345",
                    countryCode = "DE",
                    localityName = "Berlin",
                    stateOrProvinceName = "Berlin",
                    title = "Dr.",
                    domainID = listOf("vzd-cli"),
                )
            when (resourceType) {
                "base" -> {
                    printTemplate(base, outputFormat)
                }

                "entry" -> {
                    printTemplate(
                        DirectoryEntry(
                            directoryEntryBase = base,
                            userCertificates =
                                listOf(
                                    UserCertificate(
                                        userCertificate = CertificateDataDER("BASE64"),
                                        description = "Benutzt Zertifikat in DES (CRT) Binärformat konfertiert nach String mittels BASE64",
                                    ),
                                ),
                        ),
                        outputFormat,
                    )
                }

                "cert" -> {
                    printTemplate(
                        UserCertificate(
                            userCertificate = CertificateDataDER("BASE64"),
                            description = "Benutzt Zertifikat in DES (CRT) Binärformat konvertiert nach String mittels BASE64",
                        ),
                        outputFormat,
                    )
                }

                else -> {
                    throw UsageError("Undefinded resource type: $resourceType")
                }
            }
        }

    private fun printTemplate(
        template: Any,
        outputFormat: RepresentationFormat,
    ) {
        when (outputFormat) {
            RepresentationFormat.JSON -> {
                when (template) {
                    is BaseDirectoryEntry -> echo(template.toJsonPretty())
                    is DirectoryEntry -> echo(template.toJsonPretty())
                    is UserCertificate -> echo(template.toJsonPretty())
                }
            }

            RepresentationFormat.YAML -> {
                echo(template.toYaml())
            }

            else -> {
                throw UsageError("Templates are not available for format: $outputFormat")
            }
        }
    }
}
