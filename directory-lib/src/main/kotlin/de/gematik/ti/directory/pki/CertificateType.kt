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

package de.gematik.ti.directory.pki

import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers
import org.bouncycastle.asn1.x509.CertificatePolicies
import org.bouncycastle.cert.X509CertificateHolder
import java.security.cert.X509Certificate

/**
 * Certificate types of the TI as defined in gemSpec_OID Tab_PKI_405 (certificatePolicies OIDs).
 */
enum class CertificateType(
    val oid: String,
    val label: String,
) {
    C_HCI_ENC("1.2.276.0.76.4.76", "C.HCI.ENC"),
    C_HCI_AUT("1.2.276.0.76.4.77", "C.HCI.AUT"),
    C_HCI_OSIG("1.2.276.0.76.4.78", "C.HCI.OSIG"),
    C_HP_ENC("1.2.276.0.76.4.74", "C.HP.ENC"),
    C_HP_AUT("1.2.276.0.76.4.75", "C.HP.AUT"),
    C_HP_QES("1.2.276.0.76.4.72", "C.HP.QES"),
    C_HP_SIG("1.2.276.0.76.4.73", "C.HP.SIG"),
    ;

    val isEncryption: Boolean get() = this == C_HCI_ENC || this == C_HP_ENC

    companion object {
        fun fromOid(oid: String): CertificateType? = values().firstOrNull { it.oid == oid }
    }
}

fun X509Certificate.policyOids(): List<String> =
    CertificatePolicies
        .fromExtensions(X509CertificateHolder(encoded).extensions)
        ?.policyInformation
        ?.map { it.policyIdentifier.id }
        ?: emptyList()

fun X509Certificate.certificateType(): CertificateType? = policyOids().firstNotNullOfOrNull { CertificateType.fromOid(it) }

/**
 * The VZD accepts only C.HCI.ENC / C.HP.ENC without digitalSignature. ECC ENC certificates carry
 * keyAgreement only, so the key usage alone does not identify an encryption certificate.
 */
fun X509Certificate.isEncryptionCertificate(): Boolean {
    val digitalSignature = keyUsage?.getOrNull(0) ?: false
    return certificateType()?.isEncryption == true && !digitalSignature
}

fun X509Certificate.hasAdmission(): Boolean = getExtensionValue(ISISMTTObjectIdentifiers.id_isismtt_at_admission.id) != null
