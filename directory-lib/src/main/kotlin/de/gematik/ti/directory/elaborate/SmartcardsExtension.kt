package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.pki.CertificateInfo

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

    val notBefore = if (cert2 != null && cert2.notBefore > cert1.notBefore) {
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
    entry.userCertificates = entry.userCertificates
        ?.filter { it.userCertificate != null }
        ?.sortedBy { it.notBefore }
    return entry.userCertificates
        ?.map { it.userCertificate?.certificateInfo }
        ?.let {
            buildList<Smartcard> {
                var cert1: CertificateInfo? = null
                it.forEach { certInfo ->
                    if (cert1 == null) {
                        cert1 = certInfo
                    } else if (certInfo != null && certInfo.publicKeyAlgorithm != cert1?.publicKeyAlgorithm) {
                        add(infereSmartcardFrom(entry, cert1!!, certInfo))
                        cert1 = null
                    } else {
                        add(infereSmartcardFrom(entry, cert1!!))
                        cert1 = certInfo
                    }
                }
                if (cert1 != null) {
                    add(infereSmartcardFrom(entry, cert1!!))
                }
            }
        }
}
