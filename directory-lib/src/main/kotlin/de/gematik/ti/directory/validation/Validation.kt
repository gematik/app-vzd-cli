/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
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
