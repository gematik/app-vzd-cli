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

import de.gematik.ti.directory.DirectoryException
import org.bouncycastle.util.encoders.Base64
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

enum class CertificateFileFormat { DER, PEM, PKCS12 }

class CertificatePasswordRequired(
    val file: Path
) : DirectoryException("Password required for PKCS#12 file $file")

private const val ASN1_SEQUENCE = 0x30
private const val ASN1_INTEGER = 0x02

/**
 * A certificate is SEQUENCE { SEQUENCE tbsCertificate ... }, a PKCS#12 PFX is SEQUENCE { INTEGER version ... }.
 */
fun detectCertificateFileFormat(bytes: ByteArray): CertificateFileFormat {
    val head = bytes.take(64).toByteArray().toString(Charsets.ISO_8859_1).trimStart()
    if (head.startsWith("-----BEGIN")) return CertificateFileFormat.PEM
    if (bytes.size < 4 || bytes[0].toInt() and 0xFF != ASN1_SEQUENCE) return CertificateFileFormat.DER
    val lengthByte = bytes[1].toInt() and 0xFF
    val firstElementTag = if (lengthByte < 0x80) 2 else 2 + (lengthByte and 0x7F)
    if (firstElementTag >= bytes.size) return CertificateFileFormat.DER
    return if (bytes[firstElementTag].toInt() and 0xFF == ASN1_INTEGER) CertificateFileFormat.PKCS12 else CertificateFileFormat.DER
}

fun loadCertificates(
    file: Path,
    password: () -> CharArray?,
): List<CertificateDataDER> {
    val bytes = Files.readAllBytes(file)
    val certificates =
        when (detectCertificateFileFormat(bytes)) {
            CertificateFileFormat.PKCS12 -> loadPkcs12(file, bytes, password() ?: throw CertificatePasswordRequired(file))
            CertificateFileFormat.DER, CertificateFileFormat.PEM -> loadX509(file, bytes)
        }
    return certificates.map { CertificateDataDER(Base64.toBase64String(it.encoded)) }
}

private fun loadX509(
    file: Path,
    bytes: ByteArray,
): List<X509Certificate> =
    try {
        CertificateFactory
            .getInstance("X.509")
            .generateCertificates(bytes.inputStream())
            .map { it as X509Certificate }
            .ifEmpty { throw DirectoryException("No certificate found in $file") }
    } catch (e: java.security.cert.CertificateException) {
        throw DirectoryException("Cannot read certificate file $file: ${e.message}", e)
    }

private fun loadPkcs12(
    file: Path,
    bytes: ByteArray,
    password: CharArray,
): List<X509Certificate> {
    val store = KeyStore.getInstance("PKCS12")
    try {
        store.load(bytes.inputStream(), password)
    } catch (e: IOException) {
        throw DirectoryException("Cannot open PKCS#12 file $file: wrong password or corrupt file", e)
    }
    val found = linkedMapOf<String, X509Certificate>()
    fun add(cert: X509Certificate) {
        if (cert.basicConstraints == -1) found.putIfAbsent(Base64.toBase64String(cert.encoded), cert)
    }
    store.aliases().asSequence().forEach { alias ->
        store.getCertificateChain(alias)?.forEach { add(it as X509Certificate) }
        (store.getCertificate(alias) as? X509Certificate)?.let(::add)
    }
    if (found.isEmpty()) throw DirectoryException("No end-entity certificate found in PKCS#12 file $file")
    return found.values.toList()
}
