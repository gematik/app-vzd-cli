package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.gem.command.CommandsBuilder;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import generated.CommandType;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AddDirEntryExecutionIntegrationTest {

    private static final String[] TEST_ARGS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "IntegrationConfig.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "src" + File.separator + "test" + File.separator + "resources" + File.separator
        + "config" + File.separator + "commands" + File.separator + "addDir.xml"};
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
    public void addCommandSuccessTest() throws Exception {
        AddDirEntryExecution addDirEntryExecution = new AddDirEntryExecution(ConnectionPool.createConnectionPool(1));
        assertTrue(addDirEntryExecution.createCallable(commands.get(0)).call());
    }

    @Test
    public void addCommandFailTest() throws Exception {
        AddDirEntryExecution addDirEntryExecution = new AddDirEntryExecution(ConnectionPool.createConnectionPool(1));
        assertFalse(addDirEntryExecution.createCallable(commands.get(1)).call());
    }

    @Test
    public void executeCommandExceptionTest() {
        AddDirEntryExecution addDirEntryExecution = new AddDirEntryExecution(ConnectionPool.createConnectionPool(1));
        ApiException exception = assertThrows(ApiException.class,
            () -> addDirEntryExecution.executeCommand(commands.get(1), addDirEntryExecution.connectionPool.getConnection()));
        assertEquals(405, exception.getCode());
    }

    @Test
    public void addCommandDifferentTelematikIdTest() throws Exception {
        AddDirEntryExecution addDirEntryExecution = new AddDirEntryExecution(ConnectionPool.createConnectionPool(1));
        assertFalse(addDirEntryExecution.createCallable(commands.get(2)).call());
    }

    @Test
    public void executeCommandDifferentTelematikIdTest() {
        AddDirEntryExecution addDirEntryExecution = new AddDirEntryExecution(ConnectionPool.createConnectionPool(1));
        Exception exception = assertThrows(CommandException.class,
            () -> addDirEntryExecution.executeCommand(commands.get(2), addDirEntryExecution.connectionPool.getConnection()));
        assertEquals("At least two different TelematikIds found in command -> [OtherFailTelematikId, FailTelematikId]", exception.getMessage());
    }

}
