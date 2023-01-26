package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.elaborate.validation.validate
import de.gematik.ti.directory.fhir.HealthcareServiceSpecialtyVS
import de.gematik.ti.directory.fhir.OrganizationProfessionOID
import de.gematik.ti.directory.fhir.PractitionerProfessionOID
import de.gematik.ti.directory.fhir.PractitionerQualificationVS

fun DirectoryEntry.elaborate(): ElaborateDirectoryEntry {
    val entry = this
    val base = entry.directoryEntryBase.elaborate()
    val baseValidationResult = base.validate()
    val validationResult = if (baseValidationResult?.isNotEmpty() == true) {
        ValidationResult(base = baseValidationResult)
    } else {
        null
    }
    return ElaborateDirectoryEntry(
        kind = infereKind(),
        base = base,
        userCertificates = entry.userCertificates?.mapNotNull { it },
        kimAddresses = infereKIMAddresses(),
        smartcards = infereSmartcards(),
        validationResult = validationResult,
    )
}

fun BaseDirectoryEntry.elaborate(): ElaborateBaseDirectoryEntry {
    val base = this
    return ElaborateBaseDirectoryEntry(
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
        specialization = base.specialization?.map { elaborateSpecialization(base, it) },

        holder = base.holder?.map { ElaborateHolder(it, it) },
        dataFromAuthority = base.dataFromAuthority,
        personalEntry = base.personalEntry,
        changeDateTime = base.changeDateTime,

        maxKOMLEadr = base.maxKOMLEadr,

        active = base.active,
        meta = base.meta,
    )
}

fun elaborateProfessionOID(base: BaseDirectoryEntry, professionOID: String): ElaborateProfessionOID {
    val display = if (base.personalEntry == true) {
        PractitionerProfessionOID.displayFor(professionOID)
    } else {
        OrganizationProfessionOID.displayFor(professionOID)
    } ?: professionOID
    return ElaborateProfessionOID(professionOID, display)
}

val PractitionerSpecializationRegex = Regex("^urn:as:([0-9\\.]+):(.*)$")
val OrganisationSpecializationRegex = Regex("^urn:psc:([0-9\\.]+):(.*)$")

fun elaborateSpecialization(base: BaseDirectoryEntry, specialization: String): ElaborateSpecialization {
    val display = if (PractitionerSpecializationRegex.matches(specialization)) {
        PractitionerSpecializationRegex.matchEntire(specialization)?.let {
            PractitionerQualificationVS.displayFor("urn:oid:${it.groupValues[1]}", it.groupValues[2])
        } ?: specialization
    } else if (OrganisationSpecializationRegex.matches(specialization)) {
        OrganisationSpecializationRegex.matchEntire(specialization)?.let {
            HealthcareServiceSpecialtyVS.displayFor("urn:oid:${it.groupValues[1]}", it.groupValues[2])
        } ?: specialization
    } else {
        specialization
    }
    return ElaborateSpecialization(specialization, display)
}
