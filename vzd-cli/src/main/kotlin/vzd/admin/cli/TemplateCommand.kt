package vzd.admin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.choice
import vzd.admin.client.BaseDirectoryEntry
import vzd.admin.client.CertificateDataDER
import vzd.admin.client.DirectoryEntry
import vzd.admin.client.UserCertificate

class TempolateCommand : CliktCommand(name = "template", help = """Create template for a resource
     
     Supported types: base, entry, cert
""") {
    private val context by requireObject<CommandContext>()
    private val resourceType by argument(help = "Specify type of a resource").choice("base", "entry", "cert")

    override fun run() = catching {
        val base = BaseDirectoryEntry(
            telematikID = "1-x.1234567890",
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
            domainID = listOf("vzd-cli")
        )
        when (resourceType) {
            "base" -> {
                printTemplate(base, context.outputFormat)
            }
            "entry" -> {
                printTemplate(
                    DirectoryEntry(
                        directoryEntryBase = base,
                        userCertificates = listOf(
                            UserCertificate(
                                userCertificate = CertificateDataDER("BASE64"),
                                description = "Benutzt Zertifikat in DES (CRT) Binärformat konfertiert nach String mittels BASE64"

                            )
                        )
                    ), context.outputFormat)

            }
            "cert" -> {
                printTemplate(UserCertificate(
                    userCertificate = CertificateDataDER("BASE64"),
                    description = "Benutzt Zertifikat in DES (CRT) Binärformat konfertiert nach String mittels BASE64"

                ), context.outputFormat)
            }
            else -> throw UsageError("Undefinded resource type: $resourceType")
        }


    }

    private inline fun <reified T> printTemplate(template: T, outputFormat: OutputFormat) {
        when (outputFormat) {
            OutputFormat.JSON -> Output.printJson(template)
            OutputFormat.HUMAN, OutputFormat.YAML -> Output.printYaml(template)
            else -> throw UsageError("Templates are not available for format: ${context.outputFormat}")
        }
    }
}
