package de.gematik.ti.epa.vzd.gem.command;

import de.gematik.ti.epa.vzd.client.model.BaseDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.DirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.DistinguishedName;
import javax.xml.datatype.DatatypeConfigurationException;
import org.junit.Test;

public class TransformerTest {


    @Test
    public void transformEmptyBaseDirectoryEntryToCommandTypeCorrectly() throws DatatypeConfigurationException {
        DirectoryEntry directoryEntry = new DirectoryEntry();
        BaseDirectoryEntry baseDirectoryEntry = new BaseDirectoryEntry();
        DistinguishedName distinguishedName = new DistinguishedName();
        distinguishedName.setUid("SomeUid");
        baseDirectoryEntry.setDn(distinguishedName);
        directoryEntry.setDirectoryEntryBase(baseDirectoryEntry);

        Transformer.getCommandTypeFromDirectoryEntry(directoryEntry);
    }

}
