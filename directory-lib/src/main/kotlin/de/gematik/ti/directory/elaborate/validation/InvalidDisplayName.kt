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

package de.gematik.ti.directory.elaborate.validation

import de.gematik.ti.directory.elaborate.ElaborateBaseDirectoryEntry
import de.gematik.ti.directory.validation.FindingSeverity
import de.gematik.ti.directory.validation.ValidationRule

object InvalidDisplayName : ValidationRule<ElaborateBaseDirectoryEntry>({
    if (it.displayName?.trim() == "-") {
        addFinding(ElaborateBaseDirectoryEntry::displayName, FindingSeverity.ERROR)
    } else if (it.displayName?.matches(Regex("^[0-9]*\$")) == true) {
        addFinding(ElaborateBaseDirectoryEntry::displayName, FindingSeverity.ERROR)
    }
})
