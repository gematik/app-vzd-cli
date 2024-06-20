package de.gematik.ti.directory.fhir

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Reference

class TestClient : FeatureSpec({
    feature("Bundle") {
        scenario("References") {
            val ref = Reference("Practitioner/123")
            val id = IdType(ref.reference)
            id.idPart shouldBe "123"
            id.resourceType shouldBe "Practitioner"
        }
    }
})
