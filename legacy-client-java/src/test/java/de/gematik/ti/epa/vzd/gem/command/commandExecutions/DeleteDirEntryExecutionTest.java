package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.DistinguishedNameType;
import org.junit.Test;

public class DeleteDirEntryExecutionTest {

    private static IConnectionPool connectionPool = mock(ConnectionPool.class);

    @Test
    public void checkValidationMissingArgumentTest() {
        DeleteDirEntryExecution deleteDirEntryExecution = new DeleteDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        assertFalse(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationHaveUID() {
        DeleteDirEntryExecution deleteDirEntryExecution = new DeleteDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("cbca60fe-8ca7-4960-990d-ec526a200582");
        command.setDn(dn);
        assertTrue(deleteDirEntryExecution.checkValidation(command));
    }
}