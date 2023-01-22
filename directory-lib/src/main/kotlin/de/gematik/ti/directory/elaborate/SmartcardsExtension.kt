package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry
import de.gematik.ti.directory.pki.CertificateInfo

private fun infereSmartcardFrom(entry: DirectoryEntry, index: Int, cert1: CertificateInfo, cert2: CertificateInfo? = null): Smartcard {
    val smartcardType = if (cert2 != null && entry.directoryEntryBase.personalEntry == true) {
        SmartcardType.HBA2_1
    } else if (cert2 != null) {
        SmartcardType.SMCB2_1
    } else if (entry.directoryEntryBase.personalEntry == true) {
        SmartcardType.HBA
    } else {
        SmartcardType.SMCB
    }

    return Smartcard(
        type = smartcardType,
        notBefore = cert1.notBefore,
        notAfter = cert1.notAfter,
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
                it.forEachIndexed { index, certInfo ->
                    if (cert1 == null) {
                        cert1 = certInfo
                    } else if (certInfo != null && certInfo.publicKeyAlgorithm != cert1?.publicKeyAlgorithm) {
                        add(infereSmartcardFrom(entry, index - 1, cert1!!, certInfo))
                        cert1 = null
                    } else {
                        add(infereSmartcardFrom(entry, index - 1, cert1!!))
                        cert1 = certInfo
                    }
                }
                if (cert1 != null) {
                    add(infereSmartcardFrom(entry, it.size - 1, cert1!!))
                }
            }
        }
}
