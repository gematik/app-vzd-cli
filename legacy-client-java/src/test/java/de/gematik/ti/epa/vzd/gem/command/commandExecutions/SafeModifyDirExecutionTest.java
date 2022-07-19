package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.DistinguishedNameType;
import org.junit.Test;

public class SafeModifyDirExecutionTest {

    private static IConnectionPool connectionPool = mock(ConnectionPool.class);

    @Test
    public void preCheckFalseWhenUidIsEmptyString() {
        SafeModifyDirEntryExecution sModDirEnt = new SafeModifyDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("");
        command.setDn(dn);

        assertTrue(!sModDirEnt.preCheck(command));
    }

    @Test
    public void preCheckIsTrue() {
        SafeModifyDirEntryExecution sModDirEnt = new SafeModifyDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("Test");
        command.setDn(dn);

        assertTrue(sModDirEnt.preCheck(command));
    }

    @Test
    public void preCheckFalseWhenUidIsMissing() {
        SafeModifyDirEntryExecution sModDirEnt = new SafeModifyDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setCn("Test");
        command.setDn(dn);

        assertTrue(!sModDirEnt.preCheck(command));
    }
}
