package de.gematik.ti.directory.elaborate

import de.gematik.ti.directory.admin.DirectoryEntry

fun DirectoryEntry.infereKIMAddresses(): List<ElaborateKIMAddress>? {
    return fachdaten?.let { fachdaten ->
        fachdaten.mapNotNull { it.fad1 }
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
    }?.flatten()?.flatten()
}
