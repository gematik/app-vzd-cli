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

import de.gematik.ti.directory.pki.CertificateDataDER

private const val ENTRY_TYPE_PRACTITIONER = 1

/**
 * Pre-fills a base entry with what the VZD does not derive itself (gemSpec_VZD Tab_VZD_Datenbeschreibung):
 * displayName and the postal address. telematikID, entryType, professionOID, cn, sn and givenName are
 * set by the VZD from the certificate and therefore left empty.
 */
fun BaseDirectoryEntry.Companion.fromCertificate(cert: CertificateDataDER): BaseDirectoryEntry {
    val info = cert.certificateInfo
    val subject = info.subjectInfo
    val isPractitioner = EntryTypeMapping.entryTypeOf(info.admissionStatement.professionOids) == ENTRY_TYPE_PRACTITIONER
    val displayName =
        if (isPractitioner && !subject.sn.isNullOrBlank() && !subject.givenName.isNullOrBlank()) {
            "${subject.sn}, ${subject.givenName}"
        } else {
            subject.cn
        }
    val hasAddress =
        listOf(subject.streetAddress, subject.postalCode, subject.localityName, subject.stateOrProvinceName).any { it != null }
    return BaseDirectoryEntry(
        telematikID = info.admissionStatement.registrationNumber,
        displayName = displayName,
        streetAddress = subject.streetAddress,
        postalCode = subject.postalCode,
        localityName = subject.localityName,
        stateOrProvinceName = subject.stateOrProvinceName,
        countryCode = if (hasAddress) subject.countryCode else null,
    )
}
