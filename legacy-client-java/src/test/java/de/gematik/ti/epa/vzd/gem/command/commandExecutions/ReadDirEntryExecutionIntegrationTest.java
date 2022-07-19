package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.gem.command.CommandsBuilder;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import generated.CommandType;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReadDirEntryExecutionIntegrationTest {

    private static final String[] TEST_ARGS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "IntegrationConfig.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "src" + File.separator + "test" + File.separator + "resources" + File.separator
        + "config" + File.separator + "commands" + File.separator + "readDir.xml"};
    public static List<CommandType> commands;

    @Before
    public void initConfigHandler() {
        ConfigHandler.setConfigHandler(null);
        ConfigHandler.init(TEST_ARGS);
        ExecutionCollection.init(ConnectionPool.createConnectionPool(1));
        commands = new CommandsBuilder().buildCommands();
    }

    @After
    public void unsetConfigHandler() {
        ConfigHandler.setConfigHandler(null);
        ExecutionCollection.getInstance().setExecutionCollection(null);
    }

    @Test
    public void readCommandSuccessTest() throws Exception {
        ReadDirEntryExecution readDirEntryExecution = new ReadDirEntryExecution(ConnectionPool.createConnectionPool(1));
        assertTrue(readDirEntryExecution.createCallable(commands.get(0)).call());
    }

    @Test
    public void readCommandFailTest() throws Exception {
        ReadDirEntryExecution readDirEntryExecution = new ReadDirEntryExecution(ConnectionPool.createConnectionPool(1));
        assertFalse(readDirEntryExecution.createCallable(commands.get(1)).call());
    }

    @Test
    public void executeCommandIdNotPresentTest() {
        ReadDirEntryExecution readDirEntryExecution = new ReadDirEntryExecution(ConnectionPool.createConnectionPool(1));
        ApiException exception = assertThrows(ApiException.class,
            () -> readDirEntryExecution.executeCommand(commands.get(1), readDirEntryExecution.connectionPool.getConnection()));
        assertEquals(404, exception.getCode());
    }

    @Test
    public void notExsistingTelematikIDTest() {
        ReadDirEntryExecution readDirEntryExecution = new ReadDirEntryExecution(ConnectionPool.createConnectionPool(1));
        ApiException exception = assertThrows(ApiException.class,
            () -> readDirEntryExecution.executeCommand(commands.get(2), readDirEntryExecution.connectionPool.getConnection()));
        assertEquals(404, exception.getCode());

    }
}
