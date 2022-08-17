package de.gematik.ti.epa.vzd.gem.invoker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.exceptions.GemClientException;
import generated.CommandType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

public class ConfigHandlerTest {

    private static final String[] TEST_ARGS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Config.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "rightPath"};
    private static final String[] TEST_ARGS_MAX_CONNECTIONS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "ConnectionWithMaxConnectionDefined.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "rightPath"};
    private static final String[] TEST_ARGS_LIMIT_CONNECTIONS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "ConnectionWithLimitConnectionDefined.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "rightPath"};
    private static final String[] TEST_ARGS_WITH_COMMANDPATH_AND_PROXY_IN_FILE = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Config.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt"};
    private static final String[] TEST_ARGS_WITH_COMMANDPATH_AND_PROXY_IN_CLI = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Config.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-d", "4321", "-h", "anotherHost.com"};

    @Before
    public void resetConfigHandler() {
        ConfigHandler.setConfigHandler(null);
    }

    @Test
    public void testGetInstanceBeforeInit() {
        GemClientException exception = assertThrows(GemClientException.class,
            ConfigHandler::getInstance);
        assertEquals("A ConfigHandler have to be initialized first", exception.getMessage());
    }

    @Test
    public void doubleInitConfigHandlerTest() {
        ConfigHandler.init(TEST_ARGS);
        GemClientException exception = assertThrows(GemClientException.class, () ->
            ConfigHandler.init(TEST_ARGS));
        assertEquals("Configurations are only allowed to set once", exception.getMessage());
    }

    @Test
    public void checkRightCommandsWithCliPath() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS);

        assertEquals(new File("rightPath").getAbsolutePath(), configHandler.getCommandsPath());
    }

    @Test
    public void checkRightCommandsWithFilePath() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS_WITH_COMMANDPATH_AND_PROXY_IN_FILE);

        assertEquals(new File(FilenameUtils.separatorsToSystem("src\\test\\resources\\config\\Commands.xml")).getAbsolutePath(), configHandler.getCommandsPath());
    }

    @Test
    public void checkRightRetryOAuthPath() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS);

        assertEquals("https://to.be.defined/oauth/token", configHandler.getRetryingOAuthPath());
    }

    @Test
    public void checkRightBasePath() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS);

        assertEquals("http://[::1]:8080/OAuth2Token", configHandler.getBasePath());
    }

    @Test
    public void testGetProxyFromFile() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS_WITH_COMMANDPATH_AND_PROXY_IN_FILE);

        assertEquals("Wrong proxy", 1234, configHandler.getProxyPort());
        assertEquals("Wrong host", "testHost.de", configHandler.getProxyHost());
    }

    @Test
    public void testGetProxyOverrideFile() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS_WITH_COMMANDPATH_AND_PROXY_IN_CLI);

        assertEquals("Wrong proxy", 4321, configHandler.getProxyPort());
        assertEquals("Wrong host", "anotherHost.com", configHandler.getProxyHost());
    }

    @Test
    public void testMoreConnectionNeededThanSpecified() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS);
        List<CommandType> list = new ArrayList<>();
        CommandType command = new CommandType();
        command.setName("CommandType1");
        list.add(command);
        list.add(command);
        list.add(command);
        CommandType command2 = new CommandType();
        command2.setName("CommandType2");
        list.add(command2);
        list.add(command2);
        list.add(command2);
        configHandler.adjustConnectionCount(list);
        assertEquals(4, configHandler.getConnectionCount());
    }

    @Test
    public void testLesConnectionNeededThanSpecified() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS);
        List<CommandType> list = new ArrayList<>();
        CommandType command = new CommandType();
        command.setName("CommandType1");
        list.add(command);
        CommandType command2 = new CommandType();
        command2.setName("CommandType2");
        list.add(command2);
        configHandler.adjustConnectionCount(list);
        assertEquals(2, configHandler.getConnectionCount());
    }

    @Test
    public void testMoreThan20Commands() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS_MAX_CONNECTIONS);
        List<CommandType> list = new ArrayList<>();
        CommandType command = new CommandType();
        command.setName("CommandType1");
        for (int i = 0; i < 30; i++) {
            list.add(command);
        }
        CommandType command2 = new CommandType();
        command2.setName("CommandType2");
        for (int i = 0; i < 25; i++) {
            list.add(command2);
        }
        CommandType command3 = new CommandType();
        command3.setName("CommandType3");
        list.add(command3);
        CommandType command4 = new CommandType();
        command4.setName("CommandType4");
        list.add(command4);
        list.add(command4);
        list.add(command4);
        CommandType command5 = new CommandType();
        command5.setName("CommandType5");
        list.add(command5);
        list.add(command5);
        list.add(command5);
        configHandler.adjustConnectionCount(list);
        assertEquals(47, configHandler.getConnectionCount());
    }

    @Test
    public void testConnectionLimitByCommands() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS_LIMIT_CONNECTIONS);
        List<CommandType> commands = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            CommandType command = new CommandType();
            command.setName("CommandType");
            commands.add(command);
        }
        configHandler.adjustConnectionCount(commands);
        assertEquals(2, configHandler.getInstance().getConnectionCount());
    }

    @Test
    public void testMoreThanPossibleConnectionDefined() {
        ConfigHandler configHandler = ConfigHandler.init(TEST_ARGS_MAX_CONNECTIONS);
        assertEquals(ExecutionCollection.getInstance().getExecutors().size() * configHandler.getMaxParaExecutionsPerExecutor(),
            configHandler.getConnectionCount());
    }
}
