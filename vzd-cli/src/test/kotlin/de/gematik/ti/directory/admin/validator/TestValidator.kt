package de.gematik.ti.directory.admin.validator

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.DistinguishedName
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.fhir.OrganizationProfessionOID
import de.gematik.ti.directory.util.CertificateDataDER
import io.konform.validation.*
import io.konform.validation.jsonschema.maxItems
import io.konform.validation.jsonschema.pattern
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TestValidator : FeatureSpec({
    feature("TelematikID") {
        scenario("TelematikID Regex") {
            val pattern = Regex("^[0-9]+-[._\\-\\p{L}0-9]+$")
            "1-1".matches(pattern) shouldBe true
            "11-1".matches(pattern) shouldBe true
            "a-1".matches(pattern) shouldBe false
            "1-123.a_b-cüäß".matches(pattern) shouldBe true
            "1-123.456\n".matches(pattern) shouldBe false
        }
    }

    feature("Validate using Kotlin validation framework") {
        scenario("Validate invalid telematikID") {
            val entry2 = DirectoryEntry(BaseDirectoryEntry("1-abc\n"))
            val validateEntry = Validation {
                DirectoryEntry::directoryEntryBase {
                    BaseDirectoryEntry::telematikID {
                        pattern(Regex("^[0-9]+-[._\\-\\p{L}0-9]+$"))
                    }
                }
            }
            val validationResult = validateEntry(entry2)
            if (validationResult is Invalid) {
                validationResult.errors.size shouldBe 1
                validationResult[DirectoryEntry::directoryEntryBase, BaseDirectoryEntry::telematikID]?.size shouldBe 1
            }
        }

        scenario("Validate certificates") {
            val certData =
                "MIIDgDCCAyegAwIBAgIHAaW81FFB9jAKBggqhkjOPQQDAjCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E5IFRFU1QtT05MWTAeFw0yMDAxMjcwMDAwMDBaFw0yNDEyMTEyMzU5NTlaMIHVMQswCQYDVQQGEwJERTERMA8GA1UEBwwIVMO2bm5pbmcxDjAMBgNVBBEMBTI1ODMyMRMwEQYDVQQJDApBbSBNYXJrdCAxMS0wKwYDVQQKDCRQcmF4aXMgTGlsbyBHcsOkZmluIGRlIEJvZXJOT1QtVkFMSUQxDTALBgNVBAQMBEJvZXIxDTALBgNVBCoMBExpbG8xEjAQBgNVBAwMCVByb2YuIERyLjEtMCsGA1UEAwwkUHJheGlzIExpbG8gR3LDpGZpbiBkZSBCb2VyVEVTVC1PTkxZMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABE+qn0H8KYaa4IszxE3FLWH9/V58z2iYu7hUVfe7PBOQKNpBw+c6wO710QhZFLr35Ks9GQGN2IBtpITcoWsR7ZKjggEZMIIBFTAdBgNVHQ4EFgQU48MFLtrb7L8kGQ1BZpNrDgGpDUEwDgYDVR0PAQH/BAQDAgMIMCAGA1UdIAQZMBcwCgYIKoIUAEwEgSMwCQYHKoIUAEwETDAfBgNVHSMEGDAWgBRiiJrE3vyj85M5y5+Q5xOaPYnMdTA4BggrBgEFBQcBAQQsMCowKAYIKwYBBQUHMAGGHGh0dHA6Ly9laGNhLmdlbWF0aWsuZGUvb2NzcC8wDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAWDBRCZXRyaWVic3N0w6R0dGUgQXJ6dDAJBgcqghQATAQyEyExLVNNQy1CLVRlc3RrYXJ0ZS04ODMxMTAwMDAxMTcwMzUwCgYIKoZIzj0EAwIDRwAwRAIge+TDJbeTVwj3bV78Vl9ycVZx5FhxkQYVBl6JoOwo6/wCID54Dvjk0aFAMutqYdKce00bQCaGRRzzm9Ck0dsqGfaK"
            val entry = DirectoryEntry(
                BaseDirectoryEntry("1-123456789\n", specialization = listOf("foo")),
                userCertificates = listOf(UserCertificate(dn = DistinguishedName("0987654321"), userCertificate = CertificateDataDER(certData))),
            )
            fun ValidationBuilder<String>.matchesValueSet() =
                addConstraint("must match valueset") { OrganizationProfessionOID.displayFor(it) != it }

            fun ValidationBuilder<String>.validTelematikIDRest() =
                pattern(Regex("^[0-9]+-[._\\-\\p{L}0-9]+$")) hint "Invalid TelematikID"

            val validateEntry = Validation {
                basicallyValid()
                DirectoryEntry::userCertificates ifPresent {
                    maxItems(1)
                    onEach {
                        UserCertificate::dn ifPresent {
                            DistinguishedName::uid {
                                matchesValueSet()
                            }
                        }
                    }
                }
            }

            val validationResult = validateEntry(entry)

            println(validationResult.errors.map { it.dataPath + it.message }.joinToString("\n"))
        }
    }
})
