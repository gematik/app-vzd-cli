package de.gematik.ti.directory.validator

import de.gematik.ti.directory.admin.DirectoryEntry


fun String.isNotEmptyAttribute(): Boolean = this != "-" && this != ""

enum class Severity {
    CRITICAL, ERROR, WARNING, HINT
}

data class Finding(val type: FindingType, val details: String)

class ValidatorScope(val entry: DirectoryEntry) {
    val findings = mutableListOf<Finding>()

    fun report(findingType: FindingType, details: String = "") {
        findings.add(Finding(findingType, details))
    }
}

class Validator(val name: String, val validate: ValidatorScope.() -> Unit)

abstract class ValidatorSpec(body: ValidatorSpec.() -> Unit = {}) {
    private val validators = mutableListOf<Validator>()

    init {
        body()
    }

    fun validator(name: String, validate: ValidatorScope.() -> Unit) =
        validators.add(Validator(name, validate))

    fun runWith(entry: DirectoryEntry): List<Finding> {
        val findings = mutableListOf<Finding>()
        validators.forEach {
            val scope = ValidatorScope(entry)
            it.validate(scope)
            findings.addAll(scope.findings)
        }
        return findings
    }
}