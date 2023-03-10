package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.pki.CertificateInfo
import kotlin.math.abs

private fun infereSmartcardFrom(entry: DirectoryEntry, cert1: CertificateInfo, cert2: CertificateInfo? = null): Smartcard {
    val smartcardType = if (cert2 != null && entry.directoryEntryBase.personalEntry == true) {
        SmartcardType.HBA2_1
    } else if (cert2 != null) {
        SmartcardType.SMCB2_1
    } else if (entry.directoryEntryBase.personalEntry == true) {
        SmartcardType.HBA
    } else {
        SmartcardType.SMCB
    }

    val notBefore = if (cert2 != null && cert2.notBefore < cert1.notBefore) {
        cert2.notBefore
    } else {
        cert1.notBefore
    }

    val notAfter = if (cert2 != null && cert2.notAfter > cert1.notAfter) {
        cert2.notAfter
    } else {
        cert1.notAfter
    }

    return Smartcard(
        type = smartcardType,
        notBefore = notBefore,
        notAfter = notAfter,
        active = entry.userCertificates?.first { it.userCertificate?.certificateInfo?.serialNumber == cert1.serialNumber }?.active ?: false,
        certificateSerialNumbers = if (cert2 != null) listOf(cert1.serialNumber, cert2.serialNumber) else listOf(cert1.serialNumber),
    )
}

fun DirectoryEntry.infereSmartcards(): List<Smartcard>? {
    val entry = this

    val certs = entry.userCertificates?.mapNotNull { it.userCertificate?.certificateInfo }?.sortedBy { it.notBefore } ?: emptyList()

    val certPairs = identifyCertificatePairs(certs)

    return certPairs.map { infereSmartcardFrom(entry, it.first, it.second) }
}

/**
 * Small algorithm to identify the pairs of certificates bases on certificate issue date (notBefore).
 * The close ECC and RSA certificates are, they might build a pair.
 */
fun identifyCertificatePairs(certs: List<CertificateInfo>): List<Pair<CertificateInfo, CertificateInfo?>> {
    val pairs = certs.filter { it.publicKeyAlgorithm == "RSA" }.sortedBy { it.notBefore }.map { Pair<CertificateInfo,CertificateInfo?>(it, null) }.toTypedArray()
    // iterate all EC certificates and try to find the closest RSA cert to form a pair
    certs.filter { it.publicKeyAlgorithm == "EC" }.sortedBy { it.notBefore }.forEach {eccert ->
        var closestIndex = -1
        var closestPair: Pair<CertificateInfo, CertificateInfo?>? = null
        pairs.forEachIndexed { index, pair ->
            if (closestPair == null || abs(closestPair!!.first.notBefore.epochSeconds-eccert.notBefore.epochSeconds) > abs(pair.first.notBefore.epochSeconds-eccert.notBefore.epochSeconds)) {
                closestIndex = index
                closestPair = pair
            }
        }
        if (closestIndex != -1) {
            pairs[closestIndex] = Pair(closestPair!!.first, eccert)
        }
    }

    return pairs.toList()
}