package de.gematik.ti.directory.admin.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class ParameterOptions : OptionGroup(name = "Query parameters") {
    val name by option()
    val uid by option()
    val givenName by option("--givenName")
    val sn by option()
    val cn by option()
    val displayName by option("--displayName")
    val streetAddress by option("--streetAddress")
    val postalCode by option("--postalCode")
    val countryCode by option("--countryCode")
    val localityName by option("--localityName")
    val stateOrProvinceName by option("--stateOrProvinceName")
    val title by option()
    val organization by option()
    val otherName by option("--otherName")
    val telematikID by option("--telematikID", "-t")
    val specialization by option()
    val domainID by option("--domainID")
    val holder by option()
    val personalEntry by option("--personalEntry").choice("true", "false")
    val dataFromAuthority by option("--dataFromAuthority").choice("true", "false")
    val professionOID by option("--professionOID")
    val entryType by option("--entryType").int()
    val maxKOMLEadr by option("--maxKOMLEadr").int()
    val changeDateTimeFrom by option("--changeDateTimeFrom")
    val changeDateTimeTo by option("--changeDateTimeTo")
    val baseEntryOnly by option("--baseEntryOnly").choice("true", "false")

    fun toMap(): Map<String, String> {
        val self = this
        return buildMap<String, String>() {
            for (prop in ParameterOptions::class.memberProperties) {
                if (prop.visibility == KVisibility.PUBLIC && !prop.name.startsWith("group")) {
                    prop.get(self)?.run {
                        put(prop.name, this.toString())
                    }
                }
            }
        }
    }
}
