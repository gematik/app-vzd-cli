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

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers
import org.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
import org.bouncycastle.asn1.isismtt.x509.Admissions
import org.bouncycastle.asn1.isismtt.x509.ProfessionInfo
import org.bouncycastle.asn1.x500.DirectoryString
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.CertificatePolicies
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.PolicyInformation
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import kotlin.time.Duration.Companion.days

/**
 * Certificates for tests: the expired gematik test cards bundled in vzd-cli/testdata
 * and self-signed synthetic certificates with gematik extensions that are valid now.
 */
object TestCertificates {
    val testdata: Path =
        listOf("testdata", "../vzd-cli/testdata")
            .map { Paths.get(it) }
            .first { Files.isDirectory(it) }

    val smcbRsaEnc: Path = testdata.resolve("1-SMC-B-Testkarte-883110000117035-787024799120720.der")
    val smcbEccEnc: Path = testdata.resolve("1-SMC-B-Testkarte-883110000117035-463705411240438.der")
    val hbaEnc: Path = testdata.resolve("ecc-ID007-c.hp.enc-bptk-beide_professions.crt")

    val smcbValidAt: Instant = Instant.parse("2023-06-01T00:00:00Z")

    const val PHARMACY_TELEMATIK_ID = "3-01.2.2023000"
    const val OID_PUBLIC_PHARMACY = "1.2.276.0.76.4.54"

    fun load(path: Path): CertificateDataDER = CertificateDataDER(Base64.toBase64String(Files.readAllBytes(path)))

    fun toDer(cert: X509Certificate): CertificateDataDER = CertificateDataDER(Base64.toBase64String(cert.encoded))

    fun toPem(cert: X509Certificate): String =
        "-----BEGIN CERTIFICATE-----\n" + Base64.toBase64String(cert.encoded).chunked(64).joinToString("\n") + "\n-----END CERTIFICATE-----\n"

    fun writeDer(
        cert: X509Certificate,
        dir: Path,
        name: String,
    ): Path = dir.resolve(name).also { Files.write(it, cert.encoded) }

    fun synthetic(
        subject: String,
        policyOid: String,
        keyUsage: Int,
        telematikID: String?,
        professionOid: String = OID_PUBLIC_PHARMACY,
        notBefore: Instant = Clock.System.now() - 1.days,
        notAfter: Instant = Clock.System.now() + 1.days,
        ca: Boolean = false,
    ): Pair<X509Certificate, PrivateKey> {
        val keyPair =
            KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val name = X500Name(subject)
        val builder =
            JcaX509v3CertificateBuilder(
                name,
                BigInteger(64, SecureRandom()),
                Date.from(notBefore.toJavaInstant()),
                Date.from(notAfter.toJavaInstant()),
                name,
                keyPair.public,
            )
        builder.addExtension(Extension.certificatePolicies, false, CertificatePolicies(PolicyInformation(ASN1ObjectIdentifier(policyOid))))
        builder.addExtension(Extension.keyUsage, true, KeyUsage(keyUsage))
        if (ca) builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        if (telematikID != null) {
            val professionInfo =
                ProfessionInfo(
                    null,
                    arrayOf(DirectoryString("Test profession")),
                    arrayOf(ASN1ObjectIdentifier(professionOid)),
                    telematikID,
                    null,
                )
            val admissions = Admissions(null, null, arrayOf(professionInfo))
            builder.addExtension(
                ISISMTTObjectIdentifiers.id_isismtt_at_admission,
                false,
                AdmissionSyntax(null, DERSequence(admissions)),
            )
        }
        val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        return cert to keyPair.private
    }

    fun pkcs12(
        password: CharArray,
        keyEntries: List<Pair<X509Certificate, PrivateKey>>,
        trusted: List<X509Certificate>,
    ): ByteArray {
        val store = KeyStore.getInstance("PKCS12")
        store.load(null, null)
        keyEntries.forEachIndexed { i, (cert, key) -> store.setKeyEntry("key-$i", key, password, arrayOf(cert)) }
        trusted.forEachIndexed { i, cert -> store.setCertificateEntry("trusted-$i", cert) }
        return ByteArrayOutputStream().also { store.store(it, password) }.toByteArray()
    }

    fun pharmacyEnc(): X509Certificate =
        synthetic(
            "CN=Linden Apotheke TEST,STREET=Hauptstr. 1,postalCode=10117,L=Berlin,C=DE",
            CertificateType.C_HCI_ENC.oid,
            KeyUsage.keyAgreement,
            PHARMACY_TELEMATIK_ID,
        ).first

    fun pharmacyEncWithoutAddress(): X509Certificate =
        synthetic("CN=Linden Apotheke TEST,C=DE", CertificateType.C_HCI_ENC.oid, KeyUsage.keyAgreement, PHARMACY_TELEMATIK_ID).first

    fun smcbAut(): X509Certificate =
        synthetic("CN=Linden Apotheke TEST,C=DE", CertificateType.C_HCI_AUT.oid, KeyUsage.digitalSignature, PHARMACY_TELEMATIK_ID).first
}
