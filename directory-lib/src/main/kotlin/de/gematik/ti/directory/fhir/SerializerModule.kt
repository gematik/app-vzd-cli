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
