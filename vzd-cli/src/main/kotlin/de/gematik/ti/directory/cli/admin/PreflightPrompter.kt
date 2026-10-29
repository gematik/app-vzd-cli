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

package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktError
import de.gematik.ti.directory.admin.BaseDirectoryEntry
import de.gematik.ti.directory.admin.CertificateEntryDraft
import de.gematik.ti.directory.admin.PreflightFinding
import de.gematik.ti.directory.admin.withAnswers
import de.gematik.ti.directory.validation.FindingSeverity
import java.beans.Introspector

/** Typed at a prompt that offers a suggestion, this answer leaves the attribute deliberately empty. */
const val EMPTY_ANSWER = "-"

private const val MAX_ATTEMPTS = 3

fun formatFindingsForAbort(
    draft: CertificateEntryDraft,
    interactive: Boolean = false,
): String {
    val errors = draft.findings.filter { it.severity == FindingSeverity.ERROR }
    val header =
        "Following attributes are missing or invalid" +
            (draft.sector?.let { " (sector: ${it.name}, gemILF_Pflege_VZD Tabelle ${it.table})" } ?: "") + ":"
    val lines =
        errors.map { f ->
            "  ${f.attribute}: ${f.message}" + (f.allowedValues?.let { " Allowed: ${it.joinToString(", ")}" } ?: "")
        }
    val attributes = errors.map { it.attribute }.distinct()
    val sets = attributes.map { attribute -> "-s $attribute=${errors.first { it.attribute == attribute }.suggestion ?: "VALUE"}" }
    val empties = attributes.map { "-s $it=" }
    val footer =
        "Set them with ${sets.joinToString(" ")}, mark deliberately empty with ${empties.joinToString(" ")}" +
            (if (interactive) "." else ", or run without --no-prompt.")
    return (listOf(header) + lines + footer).joinToString("\n")
}

fun promptText(finding: PreflightFinding): String =
    "${finding.attribute} (${finding.message})" +
        (finding.allowedValues?.let { "\n  Allowed: ${it.joinToString(", ")}" } ?: "") +
        "\n  Enter '$EMPTY_ANSWER' to leave it empty"

/**
 * Asks for every attribute with an ERROR finding, re-asking up to [MAX_ATTEMPTS] times while the answer
 * is still invalid; answers are applied and the preflight re-run until no fixable error remains.
 * [EMPTY_ANSWER] marks the attribute as deliberately empty, a null answer (end of input) aborts.
 */
fun promptForErrors(
    draft: CertificateEntryDraft,
    ask: (PreflightFinding, String?) -> String?,
): CertificateEntryDraft {
    var current = draft
    val attempts = mutableMapOf<String, Int>()
    while (true) {
        val pending =
            current.findings
                .filter { it.severity == FindingSeverity.ERROR && (attempts[it.attribute] ?: 0) < MAX_ATTEMPTS }
                .distinctBy { it.attribute }
        if (pending.isEmpty()) return current
        val answers =
            pending.associate { finding ->
                attempts[finding.attribute] = (attempts[finding.attribute] ?: 0) + 1
                val answer =
                    ask(finding, finding.suggestion ?: currentValue(current.base, finding.attribute))
                        ?: throw CliktError("Aborted")
                finding.attribute to answer.takeUnless { it.trim() == EMPTY_ANSWER }
            }
        current = current.withAnswers(answers)
    }
}

private fun currentValue(
    base: BaseDirectoryEntry,
    attribute: String,
): String? {
    val property = Introspector.getBeanInfo(BaseDirectoryEntry::class.java).propertyDescriptors.firstOrNull { it.name == attribute }
    return when (val value = property?.readMethod?.invoke(base)) {
        null -> null
        is List<*> -> value.joinToString(",")
        else -> value.toString()
    }
}
