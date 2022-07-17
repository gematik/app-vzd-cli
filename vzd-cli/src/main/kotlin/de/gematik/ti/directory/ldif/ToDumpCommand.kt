package de.gematik.ti.directory.ldif

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import de.gematik.ti.directory.admin.client.*
import de.gematik.ti.directory.pki.CertificateDataDER
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tongfei.progressbar.ProgressBar
import mu.KotlinLogging
import org.ldaptive.LdapEntry
import org.ldaptive.dn.Dn
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

private val jsonExtended = Json {
    serializersModule = customSerializersModule
    encodeDefaults = true
}

class ToDumpCommand : CliktCommand(help = "Convert LDIF to NDJSON dump, same as produced by admin dump") {
    private val sourceFile by argument().path(mustBeReadable = true)
    private val destFile by argument().path()
    override fun run() {
        val entries = mutableMapOf<String, DirectoryEntry>()
        val progressBar = ProgressBar("1/2 Parse", sourceFile.fileSize())
        val reader = LdifSource(BufferedReader(InputStreamReader(GZIPInputStream(sourceFile.inputStream()))))

        progressBar.use {
            reader.useEntries { ldapEntry, bytesConsumed ->
                progressBar.stepBy(bytesConsumed)
                when (ldapEntry.getAttribute("objectClass").stringValue) {
                    "domain" -> Unit
                    "vzd-entry" -> {
                        val entry = toDirectoryEntry(ldapEntry)
                        entries.put(entry.directoryEntryBase.dn!!.uid, entry)
                    }
                    "vzd-certificate" -> {
                        val entry = entries[Dn(ldapEntry.dn).getValue("uid")] ?: throw UsageError("Entry ist not available: ${ldapEntry.dn}")
                        val userCertificate = toUserCertificate(ldapEntry)
                        entry.userCertificates = (entry.userCertificates?.plus(userCertificate) ?: listOf(userCertificate))
                    }
                    "vzd-komle" -> {
                        val entry = entries[Dn(ldapEntry.dn).getValue("uid")] ?: throw UsageError("Entry ist not available: ${ldapEntry.dn}")
                        entry.fachdaten = entry.fachdaten ?: emptyList<Fachdaten>().plus(toFachdaten(ldapEntry))
                    }
                    "vzd-fad" -> {
                        val entry = entries[Dn(ldapEntry.dn).getValue("uid")] ?: throw UsageError("Entry ist not available: ${ldapEntry.dn}")
                        entry.fachdaten?.first()?.fad1 = (entry.fachdaten?.first()?.fad1 ?: emptyList()).plus(toFAD1(ldapEntry))
                    }

                    else -> {
                        logger.debug { "Unknown entry: ${ldapEntry.getAttribute("objectClass").stringValue}, dn: ${ldapEntry.dn}" }
                        // throw UsageError("Unknown entry: ${ldapEntry.getAttribute("objectClass").stringValue}, dn: ${ldapEntry.dn}")
                    }
                }
            }
            progressBar.stepTo(sourceFile.fileSize())
        }

        val progressBar2 = ProgressBar("2/2: Write", entries.size.toLong())
        val out = BufferedWriter(OutputStreamWriter(GZIPOutputStream(destFile.outputStream())))
        out.use {
            progressBar2.use {
                entries.values.forEach { entry ->
                    progressBar2.stepBy(1)
                    out.write(jsonExtended.encodeToString(entry))
                    out.newLine()
                }
            }
        }
    }
}

private fun propertyName(ldapAttrName: String): String {
    return when (ldapAttrName) {
        "l" -> "localityName"
        "st" -> "stateOrProvinceName"
        "street" -> "streetAddress"
        else -> ldapAttrName
    }
}

private fun setAttributes(baseDirectoryEntry: BaseDirectoryEntry, ldapEntry: LdapEntry) {
    ldapEntry.attributes.forEach { attr ->
        val propertyName = propertyName(attr.name)

        if (propertyName == "objectClass") {
            return@forEach
        }

        if (propertyName == "uid") {
            baseDirectoryEntry.dn = DistinguishedName(uid = attr.stringValue, dc = listOf("vzd", "telematik"))
            return@forEach
        }

        val property = BaseDirectoryEntry::class.memberProperties
            .filterIsInstance<KMutableProperty<*>>()
            .firstOrNull() { it.name == propertyName }

        if (property == null) {
            logger.error { "Unknown BaseDirectoryEntry property: $propertyName" }
            return@forEach
        }

        if (property.returnType == typeOf<String>() || property.returnType == typeOf<String?>()) {
            val value = if (attr.stringValue == "") HIDDEN_VALUE else attr.stringValue
            property.setter.call(baseDirectoryEntry, value)
        } else if (property.returnType == typeOf<Int>() || property.returnType == typeOf<Int?>()) {
            property.setter.call(baseDirectoryEntry, attr.stringValue.toInt())
        } else if (property.returnType == typeOf<Boolean>() || property.returnType == typeOf<Boolean?>()) {
            property.setter.call(baseDirectoryEntry, attr.stringValue.toBoolean())
        } else if (property.returnType == typeOf<List<String>>() || property.returnType == typeOf<List<String>?>()) {
            property.setter.call(baseDirectoryEntry, attr.stringValues)
        } else {
            throw UsageError("Unsupported property type '${attr.name}': ${property.returnType}")
        }
    }
}

private fun toDirectoryEntry(ldapEntry: LdapEntry): DirectoryEntry {
    val telematikID = ldapEntry.attributes.firstOrNull { it.name == "telematikID" }?.stringValue.let {
        if (it != null) {
            "$it****"
        } else {
            ldapEntry.dn
        }
    }
    val baseDirectoryEntry = BaseDirectoryEntry(telematikID)
    setAttributes(baseDirectoryEntry, ldapEntry)
    baseDirectoryEntry.telematikID = telematikID
    return DirectoryEntry(directoryEntryBase = baseDirectoryEntry)
}

private fun toUserCertificate(ldapEntry: LdapEntry): UserCertificate {
    val userCertificate = UserCertificate()

    val dn = Dn(ldapEntry.dn)
    userCertificate.dn = DistinguishedName(
        uid = dn.getValue("uid"),
        dc = dn.getValues("dc").toList(),
        cn = dn.getValue("cn")
    )

    userCertificate.entryType = ldapEntry.getAttribute("entryType")?.stringValue
    userCertificate.telematikID = (ldapEntry.getAttribute("telematikID").stringValue ?: "") + "****"
    userCertificate.professionOID = ldapEntry.getAttribute("professionOID")?.stringValues?.toList()
    userCertificate.usage = ldapEntry.getAttribute("usage")?.stringValues?.toList()

    ldapEntry.attributes.firstOrNull { it.name == "userCertificate" }?.let {
        userCertificate.userCertificate = CertificateDataDER(Base64.getEncoder().encodeToString("<hidden>".toByteArray()))
    }

    userCertificate.description = ldapEntry.getAttribute("description")?.stringValue

    return userCertificate
}

private fun toFachdaten(ldapEntry: LdapEntry): Fachdaten {
    val dn = Dn(ldapEntry.dn)
    val fachdten = Fachdaten(
        DistinguishedName(
            uid = dn.getValue("uid"),
            dc = dn.getValues("dc").toList(),
            ou = dn.getValues("ou").toList()
        )
    )
    return fachdten
}

private fun toFAD1(ldapEntry: LdapEntry): FAD1 {
    val dn = Dn(ldapEntry.dn)
    val fad1 = FAD1(
        dn = DistinguishedName(
            uid = dn.getValue("uid"),
            dc = dn.getValues("dc").toList(),
            ou = dn.getValues("ou").toList()
        ),
        mail = ldapEntry.getAttribute("mail").stringValues?.map { "*****" + it }
    )
    return fad1
}
