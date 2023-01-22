package de.gematik.ti.directory.bff

import de.gematik.ti.directory.admin.DistinguishedName
import de.gematik.ti.directory.admin.UserCertificate
import de.gematik.ti.directory.admin.readDirectoryEntryByTelematikID
import de.gematik.ti.directory.elaborate.*
import de.gematik.ti.directory.elaborate.validation.validate
import de.gematik.ti.directory.pki.AdmissionStatementInfo
import de.gematik.ti.directory.pki.NameInfo
import de.gematik.ti.directory.pki.OCSPResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RestrictedDirectoryEntry(
    val kind: DirectoryEntryKind,
    val base: ElaborateBaseDirectoryEntry,
    val userCertificateInfos: List<RestrictedCertificateInfo>? = null,
    val kimAddresses: List<ElaborateKIMAddress>? = null,
    val smartcards: List<Smartcard>? = null,
    val validationResult: ValidationResult? = null,
)

/**
 * Short certificate info (sans base64 data)
 */
@Serializable
class RestrictedCertificateInfo(
    val dn: DistinguishedName?,
    val active: Boolean,
    val subjectInfo: NameInfo?,
    val admissionStatement: AdmissionStatementInfo?,
    val issuer: String?,
    val publicKeyAlgorithm: String?,
    val serialNumber: String?,
    val notBefore: Instant?,
    val notAfter: Instant?,
    var ocspResponse: OCSPResponse? = null,
) {
    companion object {
        fun from(cert: UserCertificate): RestrictedCertificateInfo {
            return RestrictedCertificateInfo(
                dn = cert.dn,
                subjectInfo = cert.userCertificate?.certificateInfo?.subjectInfo,
                admissionStatement = cert.userCertificate?.certificateInfo?.admissionStatement,
                issuer = cert.userCertificate?.certificateInfo?.issuer,
                publicKeyAlgorithm = cert.userCertificate?.certificateInfo?.publicKeyAlgorithm,
                serialNumber = cert.userCertificate?.certificateInfo?.serialNumber,
                notBefore = cert.userCertificate?.certificateInfo?.notBefore,
                notAfter = cert.userCertificate?.certificateInfo?.notAfter,
                active = cert.active ?: true,
                ocspResponse = cert.userCertificate?.certificateInfo?.ocspResponse,
            )
        }
    }
}


fun Route.entryRoute() {
    get("entry/{telematikID}") {
        val telematikID = call.parameters["telematikID"] ?: throw BadRequestException()
        call.adminClient.readDirectoryEntryByTelematikID(telematikID)?.let { entry ->
            call.respond(entry.elaborate().let {
                RestrictedDirectoryEntry(
                    kind = it.kind,
                    base = it.base,
                    userCertificateInfos = it.userCertificates?.map { cert -> RestrictedCertificateInfo.from(cert)  },
                    smartcards = it.smartcards,
                    validationResult = it.validationResult,
                )
            })
        }
    }
}
