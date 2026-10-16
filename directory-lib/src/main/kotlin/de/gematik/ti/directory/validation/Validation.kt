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

package de.gematik.ti.directory.validation

import kotlin.reflect.KProperty1

open class ValidationRule<T>(
    val logic: ValidationRuleContext<T>.(value: T) -> Unit
)

class ValidationRuleContext<T>(
    val validation: Validation<T>,
    val validationRule: ValidationRule<T>
) {
    fun addFinding(
        property: KProperty1<T, *>,
        severity: FindingSeverity,
        index: Int? = null,
        key: String? = null,
    ) {
        validation.addFinding(property, Finding(validationRule.javaClass.simpleName, severity, index, key))
    }

    fun runWith(value: T) {
        validationRule.logic.invoke(this, value)
    }
}

class Validation<T>(
    val rules: List<ValidationRule<T>>,
    val value: T
) {
    private val findings = mutableMapOf<String, MutableList<Finding>>()

    fun validate(): Map<String, List<Finding>>? {
        rules.forEach {
            ValidationRuleContext(this, it).runWith(value)
        }
        return findings.ifEmpty { null }
    }

    fun addFinding(
        property: KProperty1<T, *>,
        finding: Finding,
    ) {
        findings.getOrPut(property.name) { mutableListOf() }.add(finding)
    }
}
