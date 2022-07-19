package de.gematik.ti.epa.vzd.gem.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import generated.CommandType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogHelperTest {

    private static final String[] TEST_ARGS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "IntegrationConfig.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "Credentials.txt", "-b", "src" + File.separator + "test" + File.separator + "resources" + File.separator
        + "config" + File.separator + "commands" + File.separator + "modDir.xml"};

    private static final String TEST_ID = String.format("TEST_ID%s", ((new Random()).nextInt(900) + 100));
    private static final String COMMAND_NAME = "ADD_DIR_CERT";
    private static final CommandType COMMAND = new CommandType();

    @Before
    public void initConfigHandler() {
        ConfigHandler.setConfigHandler(null);
        ConfigHandler.init(TEST_ARGS);
        COMMAND.setTelematikID(TEST_ID);
        COMMAND.setName(COMMAND_NAME);
    }

    @After
    public void unsetConfigHandler() {
        ConfigHandler.setConfigHandler(null);
        COMMAND.setTelematikID(null);
        COMMAND.setName(null);
    }

    @Test
    public void TestIfLogFileGotCreated() {
        File logFile = new File(LogHelper.LOG_SUM_DIR);
        if (logFile.exists()) {
            assertTrue(logFile.delete());
        }
        LogHelper.logCommand(CommandNamesEnum.ADD_DIR_CERT, COMMAND, true);
        assertTrue(logFile.exists());
    }

    @Test
    public void TestIfCommandExecutionIsLogged() {
        LogHelper.logCommand(CommandNamesEnum.ADD_DIR_CERT, COMMAND, true);
        String[] lineCut = new String[0];
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(LogHelper.LOG_SUM_DIR))) {
            String line;
            String lastLine = null;
            while ((line = bufferedReader.readLine()) != null) {
                lastLine = line;
            }
            if (!(lastLine == null)) {
                lineCut = lastLine.split(";");
            }
        } catch (IOException e) {
            fail("no connection to log file");
        }

        try {
            assertEquals(lineCut[2], System.getProperty("user.name"));
            assertEquals(lineCut[4], TEST_ID);
            assertEquals(lineCut[5], COMMAND_NAME);
            assertEquals(lineCut[6], "OK");
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("last line of log file is empty");
        }
    }
}
