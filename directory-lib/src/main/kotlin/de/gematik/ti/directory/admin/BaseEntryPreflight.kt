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

import de.gematik.ti.directory.elaborate.OrganisationSpecializationRegex
import de.gematik.ti.directory.elaborate.PractitionerSpecializationRegex
import de.gematik.ti.directory.fhir.HealthcareServiceTypeVS
import de.gematik.ti.directory.fhir.PharmacyTypeLDAPCS
import de.gematik.ti.directory.fhir.PractitionerQualificationVS
import de.gematik.ti.directory.fhir.Region
import de.gematik.ti.directory.fhir.SimpleValueSet
import de.gematik.ti.directory.validation.FindingSeverity

data class PreflightFinding(
    val attribute: String,
    val severity: FindingSeverity,
    val message: String,
    val suggestion: String? = null,
    val allowedValues: List<String>? = null,
)

private const val EXAMPLE_PSC = "urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"
private const val EXAMPLE_AS = "urn:as:1.2.276.0.76.5.514:011001"
private val GERMAN_POSTAL_CODE = Regex("^\\d{5}$")

private val pharmacyCodes: List<String> by lazy { PharmacyTypeLDAPCS.concept.map { "${it.code} ${it.display}" } }
private val regionCodes: List<String> by lazy { Region.concept.map { it.code } }

/**
 * Local pre-check of the rules the VZD enforces on add_Directory_Entry (gemILF_Pflege_VZD Tabelle_Plausi V001-V015
 * and the sector attribute tables). ERROR findings are what the server would reject; WARNING findings are sent anyway.
 * Attributes listed in [deliberatelyEmpty] were cleared on purpose, so their findings are downgraded to WARNING.
 */
fun preflight(
    base: BaseDirectoryEntry,
    deliberatelyEmpty: Set<String> = emptySet(),
): List<PreflightFinding> {
    val findings = mutableListOf<PreflightFinding>()
    fun error(
        attribute: String,
        message: String,
        suggestion: String? = null,
        allowedValues: List<String>? = null,
    ) = findings.add(PreflightFinding(attribute, FindingSeverity.ERROR, message, suggestion, allowedValues))
    fun warning(
        attribute: String,
        message: String,
    ) = findings.add(PreflightFinding(attribute, FindingSeverity.WARNING, message))

    if ((base.displayName?.count { it.isLetterOrDigit() } ?: 0) < 2) {
        error("displayName", "displayName must contain at least 2 alphanumeric characters (V007)", suggestion = base.displayName)
    }

    val addressGiven =
        listOf(base.streetAddress, base.postalCode, base.localityName, base.stateOrProvinceName).any { !it.isNullOrBlank() }
    if (addressGiven && base.countryCode.isNullOrBlank()) {
        error("countryCode", "countryCode is required when an address is given (ISO-3166-1 alpha-2, V015)", suggestion = "DE")
    }
    if (base.countryCode == "DE") {
        base.postalCode?.let {
            if (!GERMAN_POSTAL_CODE.matches(it)) error("postalCode", "postalCode must be a 5 digit number for countryCode DE (V002)")
        }
        base.stateOrProvinceName?.let {
            if (Region.resolveCode(it) == null) {
                error("stateOrProvinceName", "stateOrProvinceName must be a value of CodeSystem Region for countryCode DE (V001)", allowedValues = regionCodes)
            }
        }
    }

    val sector = SectorMapping.sectorOf(base.telematikID)
    if (sector == null) {
        warning("telematikID", "TelematikID '${base.telematikID}' matches no sector of gemILF_Pflege_VZD; no sector-specific checks possible")
    } else {
        val table = "${sector.name}, gemILF_Pflege_VZD Tabelle ${sector.table}"
        sector.mandatory.filter { isEmpty(base, it) }.forEach { attribute ->
            when {
                attribute != "specialization" -> error(attribute, "$attribute is mandatory for sector $table")
                sector.specializationFormat == SpecializationFormat.PHARMACY_LDAP ->
                    error(attribute, "specialization is mandatory for sector $table (PharmacyTypeLDAPCS code)", allowedValues = pharmacyCodes)
                sector.specializationFormat == SpecializationFormat.PSC ->
                    error(attribute, "specialization is mandatory for sector $table (urn:psc:<OID>:<Code> from HealthcareServiceTypeVS)", suggestion = EXAMPLE_PSC)
                sector.specializationFormat == SpecializationFormat.AS ->
                    error(attribute, "specialization is mandatory for sector $table (urn:as:<OID>:<Code> from PractitionerQualificationVS)", suggestion = EXAMPLE_AS)
                else -> error(attribute, "specialization is mandatory for sector $table")
            }
        }
        base.specialization.orEmpty().forEach { value ->
            when (sector.specializationFormat) {
                SpecializationFormat.PHARMACY_LDAP ->
                    if (PharmacyTypeLDAPCS.resolveCode(value) == null) {
                        error(
                            "specialization",
                            "specialization '$value' is not a PharmacyTypeLDAPCS code; pharmacies use the numeric LDAP codes, not the FHIR codes (V006)",
                            allowedValues = pharmacyCodes,
                        )
                    }
                SpecializationFormat.PSC ->
                    checkUrnSpecialization(value, OrganisationSpecializationRegex, HealthcareServiceTypeVS, "urn:psc", "HealthcareServiceTypeVS", EXAMPLE_PSC, ::error, ::warning)
                SpecializationFormat.AS ->
                    checkUrnSpecialization(value, PractitionerSpecializationRegex, PractitionerQualificationVS, "urn:as", "PractitionerQualificationVS", EXAMPLE_AS, ::error, ::warning)
                SpecializationFormat.NONE -> Unit
            }
        }
    }

    return findings.map {
        if (it.attribute in deliberatelyEmpty && it.severity == FindingSeverity.ERROR) it.copy(severity = FindingSeverity.WARNING) else it
    }
}

private fun checkUrnSpecialization(
    value: String,
    regex: Regex,
    valueSet: SimpleValueSet,
    prefix: String,
    valueSetName: String,
    example: String,
    error: (String, String, String?, List<String>?) -> Boolean,
    warning: (String, String) -> Boolean,
) {
    val match = regex.matchEntire(value)
    if (match == null) {
        error("specialization", "specialization '$value' must have the format $prefix:<OID>:<Code>, e.g. $example", example, null)
    } else if (valueSet.resolveCode("urn:oid:${match.groupValues[1]}", match.groupValues[2]) == null) {
        warning("specialization", "specialization '$value' is not in the local copy of $valueSetName; the server may still accept it")
    }
}

private fun isEmpty(
    base: BaseDirectoryEntry,
    attribute: String,
): Boolean =
    when (attribute) {
        "displayName" -> base.displayName.isNullOrBlank()
        "streetAddress" -> base.streetAddress.isNullOrBlank()
        "postalCode" -> base.postalCode.isNullOrBlank()
        "localityName" -> base.localityName.isNullOrBlank()
        "stateOrProvinceName" -> base.stateOrProvinceName.isNullOrBlank()
        "countryCode" -> base.countryCode.isNullOrBlank()
        "organization" -> base.organization.isNullOrBlank()
        "specialization" -> base.specialization.isNullOrEmpty()
        "domainID" -> base.domainID.isNullOrEmpty()
        else -> false
    }
