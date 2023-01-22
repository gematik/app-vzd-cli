package de.gematik.ti.directory.validator

import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

@Serializable
enum class ValidationSeverity {
    INFO,
    WARNING,
    ERROR
}

@Serializable
data class Finding(
    val path: String,
    val severity: ValidationSeverity,
    val code: String,
    val templateValues: List<String>? = null,
)

@Serializable
data class ValidationResult(
    val findings: List<Finding>? = null
) {
    fun isValid(): Boolean {
        return findings?.isNotEmpty() != true
    }
}

class Rule<T> constructor(
    val code: String,
    val severity: ValidationSeverity? = null,
    val eval: (value: T) -> Boolean,
)

class ValidationBuilder<T>(val property: KProperty1<*, *>? = null, val basePath: String? = null) {
    private val rules = mutableListOf<Rule<T>>()
    private val subValidations = mutableListOf<Validation<Any>>()

    operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit) {
        val builder = ValidationBuilder<R>(property=this).also(init)
        subValidations.add(builder.build() as Validation<Any>)
    }

    internal fun addRule(rule: Rule<T>) {
        rules.add(rule)
    }


    fun build(): Validation<T> {
        return Validation<T>(
            rules as List<Rule<T>>,
            subValidations,
            property = property?.let { property as KProperty1<T, *>},
            basePath = basePath,
        )
    }
}

class Validation<T>(
    private val rules: List<Rule<T>>,
    private val subValidations: List<Validation<Any>>,
    private val property: KProperty1<T, *>? = null,
    private val basePath: String? = null
) {

    companion object {
        operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilder<T>()
            return builder.apply(init).build()
        }
        operator fun <T> invoke(property: KProperty1<T, *>?, init: ValidationBuilder<T>.() -> Unit, basePath: String = ""): Validation<T> {
            val builder = ValidationBuilder<T>(property, basePath)
            return builder.apply(init).build()
        }
    }

    fun validate(value: T): ValidationResult {
        val findings = mutableListOf<Finding>()
        subValidations.forEach { subValidation ->
            subValidation.property?.get(value as Any)?.let {
                subValidation.validate(it).findings?.let { subFindings ->
                    findings.addAll(subFindings)
                }
            }
        }
        rules.forEach {
            if (!it.eval.invoke( value )) {
                findings.add(
                    Finding(
                        path = listOfNotNull(basePath, property?.name).joinToString("."),
                        severity = it.severity!!,
                        code = it.code
                    )
                )
            }
        }
        return ValidationResult(findings)
    }
}
