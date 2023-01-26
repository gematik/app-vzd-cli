package de.gematik.ti.directory.validation

import kotlin.reflect.KProperty1

open class ValidationRule<T>(val logic: ValidationRuleContext<T>.(value: T) -> Unit)

class ValidationRuleContext<T>(val validation: Validation<T>, val validationRule: ValidationRule<T>) {
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

class Validation<T>(val rules: List<ValidationRule<T>>, val value: T) {
    private val findings = mutableMapOf<String, MutableList<Finding>>()

    fun validate(): Map<String, List<Finding>>? {
        rules.forEach {
            ValidationRuleContext(this, it).runWith(value)
        }
        return findings.ifEmpty { null }
    }

    fun addFinding(property: KProperty1<T, *>, finding: Finding) {
        findings.getOrPut(property.name) { mutableListOf() }.add(finding)
    }
}
