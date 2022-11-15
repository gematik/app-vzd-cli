package de.gematik.ti.directory.admin

object TestData {
    val telematikID = "1-SMC-B-Testkarte-883110000117035"
    val cert1 = "MIIDgDCCAyegAwIBAgIHAaW81FFB9jAKBggqhkjOPQQDAjCBmTELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEfMB0GA1UEAwwWR0VNLlNNQ0ItQ0E5IFRFU1QtT05MWTAeFw0yMDAxMjcwMDAwMDBaFw0yNDEyMTEyMzU5NTlaMIHVMQswCQYDVQQGEwJERTERMA8GA1UEBwwIVMO2bm5pbmcxDjAMBgNVBBEMBTI1ODMyMRMwEQYDVQQJDApBbSBNYXJrdCAxMS0wKwYDVQQKDCRQcmF4aXMgTGlsbyBHcsOkZmluIGRlIEJvZXJOT1QtVkFMSUQxDTALBgNVBAQMBEJvZXIxDTALBgNVBCoMBExpbG8xEjAQBgNVBAwMCVByb2YuIERyLjEtMCsGA1UEAwwkUHJheGlzIExpbG8gR3LDpGZpbiBkZSBCb2VyVEVTVC1PTkxZMFowFAYHKoZIzj0CAQYJKyQDAwIIAQEHA0IABE+qn0H8KYaa4IszxE3FLWH9/V58z2iYu7hUVfe7PBOQKNpBw+c6wO710QhZFLr35Ks9GQGN2IBtpITcoWsR7ZKjggEZMIIBFTAdBgNVHQ4EFgQU48MFLtrb7L8kGQ1BZpNrDgGpDUEwDgYDVR0PAQH/BAQDAgMIMCAGA1UdIAQZMBcwCgYIKoIUAEwEgSMwCQYHKoIUAEwETDAfBgNVHSMEGDAWgBRiiJrE3vyj85M5y5+Q5xOaPYnMdTA4BggrBgEFBQcBAQQsMCowKAYIKwYBBQUHMAGGHGh0dHA6Ly9laGNhLmdlbWF0aWsuZGUvb2NzcC8wDAYDVR0TAQH/BAIwADBZBgUrJAgDAwRQME4wTDBKMEgwRjAWDBRCZXRyaWVic3N0w6R0dGUgQXJ6dDAJBgcqghQATAQyEyExLVNNQy1CLVRlc3RrYXJ0ZS04ODMxMTAwMDAxMTcwMzUwCgYIKoZIzj0EAwIDRwAwRAIge+TDJbeTVwj3bV78Vl9ycVZx5FhxkQYVBl6JoOwo6/wCID54Dvjk0aFAMutqYdKce00bQCaGRRzzm9Ck0dsqGfaK"
    val cert2 = "MIIFDTCCA/WgAwIBAgIHAsvLfZS9UDANBgkqhkiG9w0BAQsFADCBmjELMAkGA1UEBhMCREUxHzAdBgNVBAoMFmdlbWF0aWsgR21iSCBOT1QtVkFMSUQxSDBGBgNVBAsMP0luc3RpdHV0aW9uIGRlcyBHZXN1bmRoZWl0c3dlc2Vucy1DQSBkZXIgVGVsZW1hdGlraW5mcmFzdHJ1a3R1cjEgMB4GA1UEAwwXR0VNLlNNQ0ItQ0EyNCBURVNULU9OTFkwHhcNMjAwMTI3MDAwMDAwWhcNMjQxMjExMjM1OTU5WjCB1TELMAkGA1UEBhMCREUxETAPBgNVBAcMCFTDtm5uaW5nMQ4wDAYDVQQRDAUyNTgzMjETMBEGA1UECQwKQW0gTWFya3QgMTEtMCsGA1UECgwkUHJheGlzIExpbG8gR3LDpGZpbiBkZSBCb2VyTk9ULVZBTElEMQ0wCwYDVQQEDARCb2VyMQ0wCwYDVQQqDARMaWxvMRIwEAYDVQQMDAlQcm9mLiBEci4xLTArBgNVBAMMJFByYXhpcyBMaWxvIEdyw6RmaW4gZGUgQm9lclRFU1QtT05MWTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANTbB14v+niy2VR4hwBXdsMmKTNAfCafnQsKKPcf5TZboLKA8Q/l6C8uG8XxQlgJVKgwylcMXotAuufieLCT5cDSd0woXsko6msTx313sZ6u0HW0mnWCljRMqNs0JjEmiAfxtKcTRLmCNLre1u3CRUlIMzngT/s2y9AKWj4r5dRrfNHq7XzBhTGxSEWh0sbBzM0G7sUQJ43dbzhvj5yQEmM3S/F51bFVDrx5Z2C+/HFyTbiSZfO2dRRI9WnYDjABjDz+WgG17k9Sjgu2zogGNcmoVUu8sDQ/Wx6J9eFdZmsxN/p4eMhQ1gu0AchEAEe7TrLyAkCNh8ps03M/CPjxCAcCAwEAAaOCARkwggEVMB0GA1UdDgQWBBSPuqpbBdSbnqItqiq5VJnNvNoRwDAMBgNVHRMBAf8EAjAAMDgGCCsGAQUFBwEBBCwwKjAoBggrBgEFBQcwAYYcaHR0cDovL2VoY2EuZ2VtYXRpay5kZS9vY3NwLzAOBgNVHQ8BAf8EBAMCBDAwHwYDVR0jBBgwFoAUeunhb+oUWRYF7gPp0/0hq97p2Z4wIAYDVR0gBBkwFzAKBggqghQATASBIzAJBgcqghQATARMMFkGBSskCAMDBFAwTjBMMEowSDBGMBYMFEJldHJpZWJzc3TDpHR0ZSBBcnp0MAkGByqCFABMBDITITEtU01DLUItVGVzdGthcnRlLTg4MzExMDAwMDExNzAzNTANBgkqhkiG9w0BAQsFAAOCAQEAA6+YPRved/7mxOCJtHbFUpK9HLJz4WF1fjulTBibjO4CZ0b2ciNA6taMEFKrDsB9O2ekF5Hjx3zHqzJKV5/bdfVR4EnkenhsEL4yuZ9syQcQstfJZpLg22k1EOayOIMuPoxL9keJYZK8gkxesRn7jJUrZ7i1Gk4JeS/j346gSyVfSAjSk52GjLDJPn6g3K8KGzj4kOyDo95sg/yTEpjyY4OUSSrSHEpiEmqV0L3XmyPAm6BLTpQKdK8Kber4eJBeP5EQVM+4TDOEf2yhPgA4UAXhU3K+u/K5igRTkeVZh/T6nRU4MpacqWTIsiTTCdp6TCdDDU6oGxoyi6lYShVPFw=="
    val baseEntry = BaseDirectoryEntry(
        telematikID = "1-SMC-B-Testkarte-883110000117035",
        cn = "Praxis Lilo Gräfin de BoerTEST-ONLY",
        displayName = "Praxis Lilo Gräfin de BoerTEST-ONLY",
        streetAddress = "Am Markt 1",
        postalCode = "25832",
        countryCode = "DE",
        localityName = "Tönning",
        title = "Prof. Dr.",
        organization = "Praxis Lilo Gräfin de BoerNOT-VALID",
        specialization = emptyList(),
        domainID = emptyList(),
        holder = emptyList()
    )
}