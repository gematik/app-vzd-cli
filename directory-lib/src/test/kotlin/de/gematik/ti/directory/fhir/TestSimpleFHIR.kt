package de.gematik.ti.directory.fhir

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class TestSimpleFHIR : FeatureSpec({
    feature("Very basic FHIR parser") {
        scenario("Parse CodeSystem") {
            OrganizationProfessionOID.name shouldBe "OrganizationProfessionOID"
            OrganizationProfessionOID.concept.size shouldBeGreaterThan 5
            OrganizationProfessionOID.resolveCode("1.2.276.0.76.4.55")?.display shouldBe "Krankenhausapotheke"
        }

        scenario("Parse and use ValueSet") {
            val specialization = "urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"
            val regex = Regex("^urn:psc:([0-9\\.]+):(.*)$")
            regex.matchEntire(specialization)?.let {
                HealthcareServiceSpecialtyVS.resolveCode("urn:oid:" + it.groupValues[1], it.groupValues[2])?.display shouldBe "Allgemeinmedizin"
            }
            PractitionerQualificationVS.resolveCode("urn:oid:1.2.276.0.76.5.114", "010")?.display shouldBe "FA Allgemeinmedizin"
        }
    }
})
