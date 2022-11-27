package de.gematik.ti.directory.fhir

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class TestSimpleFHIR : FeatureSpec({
    feature("Very basic FHIR parser") {
        scenario("Parse CodeSystem") {
            OrganizationProfessionOID.name shouldBe "OrganizationProfessionOID"
            OrganizationProfessionOID.concept.size shouldBeGreaterThan 5
            OrganizationProfessionOID.displayFor("1.2.276.0.76.4.55") shouldBe "Krankenhausapotheke"
        }
    }
})
