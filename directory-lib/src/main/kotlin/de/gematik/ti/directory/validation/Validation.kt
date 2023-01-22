package de.gematik.ti.directory.validation

import kotlin.reflect.KProperty1

open class ValidationRule<T>(val logic: ValidationRuleContext<T>.(value: T) -> Unit)

class ValidationRuleContext<T>(val validation: Validation<T>, val validationRule: ValidationRule<T>) {
    fun addFinding(property: KProperty1<T, *>, severity: FindingSeverity, templateValues: List<String>? = null) {
        validation.addFinding(property, Finding(validationRule.javaClass.simpleName, severity, templateValues))
    }

    fun runWith(value: T) {
        validationRule.logic.invoke(this, value)
    }
}

class Validation<T>(val rules: List<ValidationRule<T>>, val value: T) {
    private val objectFindings = mutableListOf<Finding>()
    private val propertyFindings = mutableMapOf<String, MutableList<Finding>>()

    fun validate(): ValidationResult {
        rules.forEach {
            ValidationRuleContext(this, it).runWith(value)
        }
        return ValidationResult(
            findings = objectFindings.toList(),
            attributes = propertyFindings.entries.associate {
                it.key to ValidationResult(it.value)
            },
        )
    }

    fun addFinding(finding: Finding) {
        objectFindings.add(finding)
    }

    fun addFinding(property: KProperty1<T, *>, finding: Finding) {
        propertyFindings.getOrPut(property.name) { mutableListOf() }.add(finding)
    }

}

