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

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.elaborate.specialcases.PharmacySpecializationSpecialCase
import de.gematik.ti.directory.elaborate.validation.validate
import de.gematik.ti.directory.fhir.*

fun DirectoryEntry.elaborate(): ElaborateDirectoryEntry {
    val entry = this
    val base = entry.directoryEntryBase.elaborate()
    val elaborateEntry =
        ElaborateDirectoryEntry(
            kind = base.kind,
            base = base,
            userCertificates = entry.userCertificates?.map { it },
            kimAddresses = infereKIMAddresses(),
            smartcards = infereSmartcards(),
        )

    // apply special cases
    specialCases.forEach { it.apply(elaborateEntry) }

    val baseValidationResult = base.validate()
    val validationResult =
        if (baseValidationResult?.isNotEmpty() == true) {
            ValidationResult(base = baseValidationResult)
        } else {
            null
        }

    elaborateEntry.validationResult = validationResult

    return elaborateEntry
}

interface SpecialCase {
    fun apply(entry: ElaborateDirectoryEntry)
}

private val specialCases =
    listOf<SpecialCase>(
        PharmacySpecializationSpecialCase(),
    )

fun BaseDirectoryEntry.elaborate(): ElaborateBaseDirectoryEntry {
    val base = this
    val mapping = infereKind()
    return ElaborateBaseDirectoryEntry(
        kind = mapping.code,
        fhirResourceType = mapping.fhirResourceType,
        telematikID = base.telematikID,
        domainID = base.domainID,
        dn = base.dn,
        displayName = base.displayName,
        cn = base.cn,
        otherName = base.otherName,
        organization = base.organization,
        givenName = base.givenName,
        sn = base.sn,
        title = base.title,
        streetAddress = base.streetAddress,
        postalCode = base.postalCode,
        localityName = base.localityName,
        stateOrProvinceName = base.stateOrProvinceName,
        countryCode = base.countryCode,
        professionOID = base.professionOID?.map { elaborateProfessionOID(base, it) },
        specialization = base.specialization?.map { elaborateSpecialization(it) },
        holder = base.holder?.map { elaborateHolder(it) },
        dataFromAuthority = base.dataFromAuthority,
        personalEntry = base.personalEntry,
        changeDateTime = base.changeDateTime,
        maxKOMLEadr = base.maxKOMLEadr,
        active = base.active,
        meta = base.meta,
        providedBy = base.providedBy,
    )
}

fun elaborateProfessionOID(
    base: BaseDirectoryEntry,
    professionOID: String,
): Coding {
    val coding =
        if (base.personalEntry == true) {
            PractitionerProfessionOID.resolveCode(professionOID)
        } else {
            OrganizationProfessionOID.resolveCode(professionOID)
        }
    return Coding(professionOID, coding?.display ?: professionOID, coding?.system)
}

val PractitionerSpecializationRegex = Regex("^urn:as:([0-9\\.]+):(.*)$")
val OrganisationSpecializationRegex = Regex("^urn:psc:([0-9\\.]+):(.*)$")

fun elaborateSpecialization(specialization: String): Coding {
    val coding =
        if (PractitionerSpecializationRegex.matches(specialization)) {
            PractitionerSpecializationRegex.matchEntire(specialization)?.let {
                PractitionerQualificationVS.resolveCode("urn:oid:${it.groupValues[1]}", it.groupValues[2])
            }
        } else if (OrganisationSpecializationRegex.matches(specialization)) {
            OrganisationSpecializationRegex.matchEntire(specialization)?.let {
                HealthcareServiceTypeVS.resolveCode("urn:oid:${it.groupValues[1]}", it.groupValues[2])
            }
        } else {
            null
        }
    return coding ?: Coding(specialization, specialization)
}

fun elaborateHolder(holder: String): Coding = Holder.resolveCode(holder) ?: Coding(holder, holder)
