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

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.*

/**
 * Combined entry for all resource types
 */
@Serializable
data class FHIRDirectoryEntry(
    val telematikID: String?,
    val displayName: String?,
    val resourceType: ResourceType,
    val organization: @Contextual Organization? = null,
    val healthcareService: @Contextual HealthcareService? = null,
    val practitioner: @Contextual Practitioner? = null,
    val practitionerRole: @Contextual PractitionerRole? = null,
    val location: List<@Contextual Location>? = null,
    val endpoint: List<@Contextual Endpoint>? = null,
)
