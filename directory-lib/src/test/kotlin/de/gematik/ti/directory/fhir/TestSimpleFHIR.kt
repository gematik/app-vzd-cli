package de.gematik.ti.directory.fhir

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TestSimpleFHIR : FeatureSpec({
    feature("Very basic FHIR parser") {
        scenario("Parse CodeSystem") {
            OrganizationProfessionOID.name shouldBe "OrganizationProfessionOID"
            OrganizationProfessionOID.concept.size shouldBeGreaterThan 5
            OrganizationProfessionOID.displayFor("1.2.276.0.76.4.55") shouldBe "Krankenhausapotheke"
        }

        scenario("Parse and use ValueSet") {
            HealthcareServiceSpecialtyVS.name shouldBe "HealthcareServiceSpecialtyVS"
            HealthcareServiceSpecialtyVS.displayFor("urn:oid:1.3.6.1.4.1.19376.3.276.1.5.4", "ALLG") shouldNotBe "ALLG"
            val specialization = "urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"
            val regex = Regex("^urn:psc:([0-9\\.]+):(.*)$")
            regex.matchEntire(specialization)?.let {
                HealthcareServiceSpecialtyVS.displayFor("urn:oid:" + it.groupValues[1], it.groupValues[2]) shouldBe "Allgemeinmedizin"
            }
            PractitionerQualificationVS.displayFor("urn:oid:1.2.276.0.76.5.114", "010") shouldBe "FA Allgemeinmedizin"
        }
    }
})
