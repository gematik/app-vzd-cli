package de.gematik.ti.directory.fhir

import de.gematik.ti.directory.ClientCredentialsAuthenticator
import de.gematik.ti.directory.DirectoryEnvironment
import io.kotest.core.spec.style.FeatureSpec

class TestClient : FeatureSpec({
    feature("Authentication") {
        scenario("Authenticate using secrets from environment") {
            val client = Client()
            val env = DirectoryEnvironment.valueOf(System.getenv("FHIR_DIRECTORY_ENV"))
            val clientId = System.getenv("FHIR_DIRECTORY_CLIENT_ID")
            val clientSecret = System.getenv("FHIR_DIRECTORY_CLIENT_SECRET")

            val envConfig = DefaultConfig.environment(env)

            val auth = ClientCredentialsAuthenticator(
                envConfig.serviceAuthURL,
                null
            )

            val token = auth.authenticate(clientId, clientSecret)
            println(token)
        }
    }
})
