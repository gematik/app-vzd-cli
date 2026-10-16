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

package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry

fun DirectoryEntry.infereKIMAddresses(): List<ElaborateKIMAddress>? =
    fachdaten
        ?.let { fachdaten ->
            fachdaten
                .mapNotNull { it.fad1 }
                .map { fad1List ->
                    fad1List.map { fad1 ->
                        fad1.komLeData?.map {
                            val provider =
                                fad1.dn.ou?.first()?.let { fad ->
                                    ElaborateKIMProvider(fad, fad)
                                }
                            val kimAddress =
                                fad1.kimData?.firstOrNull { kimData ->
                                    kimData.mail == it.mail
                                }
                            ElaborateKIMAddress(
                                it.mail,
                                it.version,
                                provider,
                                kimAddress?.appTags,
                            )
                        } ?: emptyList()
                    }
                }
        }?.flatten()
        ?.flatten()
