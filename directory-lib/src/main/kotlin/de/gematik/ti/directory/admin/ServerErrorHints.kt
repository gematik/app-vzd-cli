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

package de.gematik.ti.directory.admin

import de.gematik.ti.directory.fhir.PharmacyTypeLDAPCS
import de.gematik.ti.directory.fhir.Region

/**
 * The VZD reports validation failures with terse implementation strings. This adds the allowed values or the
 * relevant mapping from gemSpec_VZD / gemILF_Pflege_VZD so the user can fix the request without reading the spec.
 * Matching is tolerant because the server texts are not specified.
 */
fun explainServerError(
    details: String,
    telematikID: String? = null,
    professionOIDs: List<String> = emptyList(),
): String? {
    val text = details.lowercase()
    return when {
        "entrytype mismatch" in text -> explainEntryType(professionOIDs)
        "healthcarespeciality mismatch" in text || "healthcarespecialty mismatch" in text -> explainSpecialization(telematikID)
        "komlecertpolicies" in text ->
            "The VZD accepts only encryption certificates (C.HCI.ENC 1.2.276.0.76.4.76 or C.HP.ENC 1.2.276.0.76.4.74) " +
                "without digitalSignature key usage. Check the certificate type with: vzd-cli admin cert-info <file>"
        "stateorprovincename" in text ->
            "stateOrProvinceName for countryCode DE must be one of: ${Region.concept.joinToString(", ") { it.code }}"
        else -> null
    }
}

private fun explainEntryType(professionOIDs: List<String>): String =
    if (professionOIDs.isNotEmpty()) {
        "The VZD derives entryType from the certificate (Tab_VZD_Mapping_Eintragstyp_und_ProfessionOID):\n" +
            professionOIDs.joinToString("\n") { oid ->
                val entryType = EntryTypeMapping.entryTypeOf(oid)
                "  ${EntryTypeMapping.describe(oid)} -> " +
                    (entryType?.let { "entryType $it (${EntryTypeMapping.nameOf(it)})" } ?: "unknown entryType")
            } + "\nOmit entryType or set it to the derived value."
    } else {
        "The VZD derives entryType from the certificate's professionOID (Tab_VZD_Mapping_Eintragstyp_und_ProfessionOID): " +
            EntryTypeMapping.entries.joinToString(", ") { "${it.entryType} ${it.name}" } +
            ". Omit entryType or set it to the derived value."
    }

private val pharmacyLine =
    "Pharmacies (SMC-B, TelematikID 3-...2 or 9-2.54): specialization is a PharmacyTypeLDAPCS code, only the code: " +
        PharmacyTypeLDAPCS.concept.joinToString(", ") { "${it.code} ${it.display}" } +
        ". FHIR codes like offizin-apotheke are not valid in the LDAP VZD."
private const val PSC_LINE = "Other SMC-B entries: urn:psc:<OID>:<Code> from HealthcareServiceTypeVS, e.g. urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"
private const val AS_LINE = "HBA entries: urn:as:<OID>:<Code> from PractitionerQualificationVS, e.g. urn:as:1.2.276.0.76.5.514:011001"

private fun explainSpecialization(telematikID: String?): String {
    val sector = telematikID?.let { SectorMapping.sectorOf(it) }
    val lines =
        when (sector?.specializationFormat) {
            SpecializationFormat.PHARMACY_LDAP -> listOf(pharmacyLine)
            SpecializationFormat.PSC -> listOf(PSC_LINE)
            SpecializationFormat.AS -> listOf(AS_LINE)
            SpecializationFormat.NONE -> listOf("specialization is not used for sector ${sector.name}")
            null -> listOf(pharmacyLine, PSC_LINE, AS_LINE)
        }
    val header = sector?.let { "Allowed specialization for sector ${it.name} (gemILF_Pflege_VZD Tabelle ${it.table}):" } ?: "Allowed specialization formats:"
    return header + "\n" + lines.joinToString("\n") { "  $it" }
}
