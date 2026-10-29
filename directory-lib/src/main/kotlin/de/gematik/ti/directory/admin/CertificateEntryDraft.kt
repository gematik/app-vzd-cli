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

import de.gematik.ti.directory.DirectoryException
import de.gematik.ti.directory.pki.CertificateDataDER
import de.gematik.ti.directory.pki.NameInfo
import de.gematik.ti.directory.pki.certificateType
import de.gematik.ti.directory.pki.hasAdmission
import de.gematik.ti.directory.pki.isEncryptionCertificate
import de.gematik.ti.directory.validation.FindingSeverity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.bouncycastle.asn1.x500.X500Name
import java.security.cert.X509Certificate

private val logger = KotlinLogging.logger {}

class CertificateSelectionException(
    message: String
) : DirectoryException(message)

/**
 * Keeps the certificates the VZD would accept: encryption type, admission present, currently valid.
 * Sorted by notBefore because the VZD takes the automatically extracted values from the last certificate.
 */
fun selectEncryptionCertificates(
    certs: List<CertificateDataDER>,
    now: Instant = Clock.System.now(),
): List<CertificateDataDER> {
    val rejected = mutableListOf<String>()
    val selected =
        certs.filter { cert ->
            val x509 = cert.certificate
            val reason = rejectionReason(x509, now)
            if (reason != null) {
                rejected += describe(x509, reason)
                logger.warn { "Ignoring certificate: ${describe(x509, reason)}" }
            }
            reason == null
        }
    if (selected.isEmpty()) {
        throw CertificateSelectionException(
            "No usable encryption certificate found:\n" + rejected.joinToString("\n") { "  $it" } +
                "\nVZD accepts only C.HCI.ENC or C.HP.ENC certificates that are currently valid.",
        )
    }
    val telematikIDs = selected.map { it.certificateInfo.admissionStatement.registrationNumber }.distinct()
    if (telematikIDs.size > 1) {
        throw CertificateSelectionException(
            "Certificates belong to different TelematikIDs: ${telematikIDs.joinToString(", ")}. Use one file per TelematikID.",
        )
    }
    val sorted = selected.sortedBy { it.certificateInfo.notBefore }
    if (sorted.map { it.certificateInfo.subject }.distinct().size > 1) {
        logger.info { "Selected certificates have different subjects; base entry is derived from the newest one" }
    }
    return sorted
}

private fun rejectionReason(
    x509: X509Certificate,
    now: Instant,
): String? {
    val notBefore = Instant.fromEpochMilliseconds(x509.notBefore.time)
    val notAfter = Instant.fromEpochMilliseconds(x509.notAfter.time)
    return when {
        !x509.hasAdmission() -> "no admission extension"
        !x509.isEncryptionCertificate() -> "not an encryption certificate (${x509.certificateType()?.label ?: "unknown type"})"
        now < notBefore -> "not valid before ${notBefore.toString().substring(0, 10)}"
        now > notAfter -> "expired ${notAfter.toString().substring(0, 10)}"
        else -> null
    }
}

private fun describe(
    x509: X509Certificate,
    reason: String,
): String {
    val cn = NameInfo(X500Name(x509.subjectX500Principal.name)).cn ?: x509.subjectX500Principal.name
    val type = x509.certificateType()?.label ?: "unknown type"
    return "$cn | $type | serial ${x509.serialNumber} | $reason"
}

data class CertificateEntryDraft(
    val base: BaseDirectoryEntry,
    val certificates: List<CertificateDataDER>,
    val entryType: Int?,
    val sector: Sector?,
    val findings: List<PreflightFinding>,
    val deliberatelyEmpty: Set<String>,
) {
    fun toCreateDirectoryEntry(): CreateDirectoryEntry =
        CreateDirectoryEntry(base, certificates.map { UserCertificate(userCertificate = it) })

    fun hasErrors(): Boolean = findings.any { it.severity == FindingSeverity.ERROR }
}

fun draftFromCertificates(
    certs: List<CertificateDataDER>,
    overrides: Map<String, String>,
    now: Instant = Clock.System.now(),
): CertificateEntryDraft {
    val selected = selectEncryptionCertificates(certs, now)
    val primary = selected.last()
    val professionOids = primary.certificateInfo.admissionStatement.professionOids
    val entryType = EntryTypeMapping.entryTypeOf(professionOids)

    overrides["entryType"]?.takeIf { it.isNotBlank() }?.let { given ->
        if (entryType != null && given.trim() != entryType.toString()) {
            throw DirectoryException(
                "Certificate professionOID ${professionOids.joinToString(", ") { EntryTypeMapping.describe(it) }} yields entryType $entryType, " +
                    "but entryType $given was given. The VZD derives entryType from the certificate; omit the option.",
            )
        }
        logger.info { "entryType $given is not sent, the VZD derives it from the certificate" }
    }

    val base = BaseDirectoryEntry.fromCertificate(primary)
    val deliberatelyEmpty = applyOverrides(base, overrides - "entryType")
    return CertificateEntryDraft(
        base = base,
        certificates = selected,
        entryType = entryType,
        sector = SectorMapping.sectorOf(base.telematikID),
        findings = preflight(base, deliberatelyEmpty),
        deliberatelyEmpty = deliberatelyEmpty,
    )
}

/**
 * Applies interactive answers; a null or blank answer means the attribute stays deliberately empty.
 */
fun CertificateEntryDraft.withAnswers(answers: Map<String, String?>): CertificateEntryDraft {
    val updated = base.copy()
    val emptied = applyOverrides(updated, answers.mapValues { it.value ?: "" })
    val filled = answers.filterValues { !it.isNullOrBlank() }.keys
    val stillEmpty = (deliberatelyEmpty - filled) + emptied
    return copy(base = updated, deliberatelyEmpty = stillEmpty, findings = preflight(updated, stillEmpty))
}
