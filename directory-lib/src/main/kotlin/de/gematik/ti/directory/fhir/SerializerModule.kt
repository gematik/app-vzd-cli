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

package de.gematik.ti.directory.fhir

import ca.uhn.fhir.context.FhirContext
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.hl7.fhir.r4.model.*

val FHIRSerializerModule =
    SerializersModule {
        contextual(Organization::class, ResourceSerializer())
        contextual(HealthcareService::class, ResourceSerializer())
        contextual(Practitioner::class, ResourceSerializer())
        contextual(PractitionerRole::class, ResourceSerializer())
        contextual(Location::class, ResourceSerializer())
        contextual(Endpoint::class, ResourceSerializer())
    }

val FhirContextR4 = FhirContext.forR4()

/**
 * Generic Serializer für FHIR Resource
 */
class ResourceSerializer<R> : KSerializer<R> where R : BaseResource {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: R,
    ) {
        // first serialize the resource inti json string
        val parser = FhirContextR4.newJsonParser()
        val json = parser.encodeResourceToString(value)
        // now parse the json string into a JsonObject
        val surrogate: JsonObject = Json.decodeFromString(json)
        // now serialize the JsonObject using kotlinx.serialization
        encoder.encodeSerializableValue(JsonObject.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): R = throw NotImplementedError()
}
