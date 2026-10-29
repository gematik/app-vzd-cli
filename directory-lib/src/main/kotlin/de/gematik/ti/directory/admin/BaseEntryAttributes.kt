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

package de.gematik.ti.directory.admin

import de.gematik.ti.directory.DirectoryException
import java.beans.Introspector

private val baseEntryProperties by lazy {
    Introspector.getBeanInfo(BaseDirectoryEntry::class.java).propertyDescriptors.filter { it.writeMethod != null }
}

fun setAttributes(
    baseDirectoryEntry: BaseDirectoryEntry,
    attrs: Map<String, String>,
) {
    attrs.forEach { (name, value) ->
        val property =
            baseEntryProperties.firstOrNull { it.name == name }
                ?: throw DirectoryException("Unknown attribute '$name'")

        when {
            property.propertyType == String::class.java -> property.writeMethod.invoke(baseDirectoryEntry, value)
            property.propertyType == Int::class.java || property.propertyType == Integer::class.java ->
                property.writeMethod.invoke(baseDirectoryEntry, value.toInt())
            property.readMethod.genericReturnType.typeName == "java.util.List<java.lang.String>" ->
                property.writeMethod.invoke(baseDirectoryEntry, value.split(',').map { it.trim() })
            property.propertyType == Boolean::class.java -> property.writeMethod.invoke(baseDirectoryEntry, value.toBoolean())
            else -> throw DirectoryException("Unsupported property type '$name': ${property.readMethod.genericReturnType.typeName}")
        }
    }
}

/**
 * Applies --set overrides. A blank value means "deliberately empty": the attribute is cleared instead of
 * being sent as "" and its name is returned so validation can downgrade a missing-value finding.
 */
fun applyOverrides(
    base: BaseDirectoryEntry,
    attrs: Map<String, String>,
): Set<String> {
    val (blank, valued) = attrs.entries.partition { it.value.isBlank() }
    blank.forEach { (name, _) ->
        val property =
            baseEntryProperties.firstOrNull { it.name == name }
                ?: throw DirectoryException("Unknown attribute '$name'")
        property.writeMethod.invoke(base, null)
    }
    setAttributes(base, valued.associate { it.key to it.value })
    return blank.map { it.key }.toSet()
}
