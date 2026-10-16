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

package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DistinguishedName
import de.gematik.ti.directory.admin.Operation
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.fhir.Coding
import de.gematik.ti.directory.validation.Finding
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ElaborateDirectoryEntry(
    val kind: String,
    val base: ElaborateBaseDirectoryEntry,
    val userCertificates: List<UserCertificate>? = null,
    val kimAddresses: List<ElaborateKIMAddress>? = null,
    val smartcards: List<Smartcard>? = null,
    var validationResult: ValidationResult? = null,
    var logs: List<ElaborateLogEntry>? = null,
)

@Serializable
data class ValidationResult(
    val base: Map<String, List<Finding>>?,
)

@Serializable
data class ElaborateBaseDirectoryEntry(
    var kind: String,
    var fhirResourceType: DirectoryEntryResourceType,
    // Identifier
    var telematikID: String,
    var domainID: List<String>? = null,
    var dn: DistinguishedName? = null,
    // Names
    var displayName: String? = null,
    var cn: String? = null,
    var otherName: String? = null,
    var organization: String? = null,
    var givenName: String? = null,
    var sn: String? = null,
    var title: String? = null,
    // Addresses
    var streetAddress: String? = null,
    var postalCode: String? = null,
    var localityName: String? = null,
    var stateOrProvinceName: String? = null,
    var countryCode: String? = null,
    // Professional
    var professionOID: List<Coding>? = null,
    var specialization: List<Coding>? = null,
    var entryType: List<String?>? = null,
    // System
    var holder: List<Coding>? = null,
    var dataFromAuthority: Boolean? = null,
    var personalEntry: Boolean? = null,
    var changeDateTime: Instant? = null,
    // Internal
    var maxKOMLEadr: Int? = null,
    // Misc
    var active: Boolean,
    var meta: List<String>? = null,
    var providedBy: String? = null,
)

enum class SmartcardType {
    HBA,
    HBA2_1,
    SMCB,
    SMCB2_1,
}

@Serializable
data class Smartcard(
    val type: SmartcardType,
    val notBefore: Instant,
    val notAfter: Instant,
    val active: Boolean,
    val certificateSerialNumbers: List<String>,
)

@Serializable
data class ElaborateKIMAddress(
    val mail: String,
    val version: String,
    val provider: ElaborateKIMProvider?,
    val appTags: List<String>? = null,
)

@Serializable
data class ElaborateKIMProvider(
    val code: String,
    val display: String,
)

@Serializable
data class ElaborateLogEntry(
    val clientID: String?,
    val logTime: Instant,
    val operation: Operation,
    val noDataChanged: Boolean,
)
