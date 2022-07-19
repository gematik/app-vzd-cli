/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem;

public enum CommandNamesEnum {

  ADD_DIR_ENTRY("addDirectoryEntries"),
  READ_DIR_ENTRY("readDirectoryEntries"),
  MOD_DIR_ENTRY("modifyDirectoryEntries"),
  SMOD_DIR_ENTRY("safeModifyDirectoryEntries"),
  DEL_DIR_ENTRY("deleteDirectoryEntries"),
  DEL_DIR_CERT("deleteDirectoryEntryCertificate"),
  ADD_DIR_CERT("addDirectoryEntryCertificate"),
  READ_DIR_CERT("readDirectoryEntryCertificate"),
  READ_DIR_ENTRY_SYNC("readDirectoryEntriesSync"),
  GET_INFO("getInfo");

  private final String name;

  CommandNamesEnum(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public static CommandNamesEnum getEntry(String name) {
    for (CommandNamesEnum cn : CommandNamesEnum.values()) {
      if (name.equals(cn.getName())) {
        return cn;
      }
    }
    return null;
  }
}