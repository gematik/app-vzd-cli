package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.gematik.ti.epa.vzd.gem.command.CommandsBuilder;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import generated.CommandType;
import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReadDirEntrySyncExecutionIntegrationTest {

    private static final String[] TEST_ARGS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "IntegrationConfig.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "src" + File.separator + "test" + File.separator + "resources" + File.separator
        + "config" + File.separator + "commands" + File.separator + "readDirSync.xml"};
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
        ReadDirEntrySyncExecution readDirEntrySyncExecution = new ReadDirEntrySyncExecution(ConnectionPool.createConnectionPool(1));
        assertTrue(readDirEntrySyncExecution.createCallable(commands.get(0)).call());
    }

    @Test
    public void receive200Entries() throws Exception {
        ReadDirEntrySyncExecution readDirEntrySyncExecution = new ReadDirEntrySyncExecution(ConnectionPool.createConnectionPool(1));
        ExecutionResult result = readDirEntrySyncExecution
            .executeCommand(commands.get(1), readDirEntrySyncExecution.connectionPool.getConnection());
        assertEquals(380624, result.getMessage().length());
        assertEquals(200, result.getHttpStatusCode());
        assertEquals(true, result.getResult());
    }
}
