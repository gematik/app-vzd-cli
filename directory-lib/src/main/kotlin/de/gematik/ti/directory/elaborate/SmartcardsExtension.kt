package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.pki.CertificateInfo
import kotlin.math.abs

private fun infereSmartcardFrom(
    entry: DirectoryEntry,
    cert1: CertificateInfo,
    cert2: CertificateInfo? = null,
): Smartcard {
    // EC-only certificates (without RSA pair) are version 2.1
    val isEcOnly = cert1.publicKeyAlgorithm == "EC" && cert2 == null
    val hasPairedCerts = cert2 != null

    val smartcardType =
        if (hasPairedCerts && entry.directoryEntryBase.personalEntry == true) {
            SmartcardType.HBA2_1
        } else if (hasPairedCerts) {
            SmartcardType.SMCB2_1
        } else if (isEcOnly && entry.directoryEntryBase.personalEntry == true) {
            SmartcardType.HBA2_1
        } else if (isEcOnly) {
            SmartcardType.SMCB2_1
        } else if (entry.directoryEntryBase.personalEntry == true) {
            SmartcardType.HBA
        } else {
            SmartcardType.SMCB
        }

    val notBefore =
        if (cert2 != null && cert2.notBefore < cert1.notBefore) {
            cert2.notBefore
        } else {
            cert1.notBefore
        }

    val notAfter =
        if (cert2 != null && cert2.notAfter > cert1.notAfter) {
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
 * EC-only certificates (without RSA pair) are also detected as standalone cards (version 2.1).
 */
fun identifyCertificatePairs(certs: List<CertificateInfo>): List<Pair<CertificateInfo, CertificateInfo?>> {
    val rsaCerts = certs.filter { it.publicKeyAlgorithm == "RSA" }.sortedBy { it.notBefore }
    val ecCerts = certs.filter { it.publicKeyAlgorithm == "EC" }.sortedBy { it.notBefore }.toMutableList()

    val pairs =
        rsaCerts
            .map { Pair<CertificateInfo, CertificateInfo?>(it, null) }
            .toMutableList()

    // iterate all EC certificates and try to find the closest RSA cert to form a pair
    val pairedEcCerts = mutableSetOf<CertificateInfo>()
    ecCerts.forEach { eccert ->
        var closestIndex = -1
        var closestPair: Pair<CertificateInfo, CertificateInfo?>? = null
        pairs.forEachIndexed { index, pair ->
            // Only pair with RSA certs that don't already have an EC cert
            if (pair.second == null &&
                (
                    closestPair == null ||
                        abs(closestPair!!.first.notBefore.epochSeconds - eccert.notBefore.epochSeconds) >
                        abs(pair.first.notBefore.epochSeconds - eccert.notBefore.epochSeconds)
                )
            ) {
                closestIndex = index
                closestPair = pair
            }
        }
        if (closestIndex != -1) {
            pairs[closestIndex] = Pair(closestPair!!.first, eccert)
            pairedEcCerts.add(eccert)
        }
    }

    // Add EC-only certificates as standalone cards (version 2.1)
    ecCerts.filter { it !in pairedEcCerts }.forEach { eccert ->
        pairs.add(Pair(eccert, null))
    }

    return pairs.toList()
}
