package de.gematik.ti.epa.vzd.gem.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.exceptions.ReadException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import java.io.File;
import org.junit.Before;
import org.junit.Test;

public class CommandsBuilderTest {

    private static final String[] ARGS_Wrong_Formatted = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "ConfigWrongFormattedCommands.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "bspCredentials.txt"};
    private static final String[] ARGS_MISSING_COMMANDS = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "ConfigMissingCommands.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "bspCredentials.txt"};
    private static final String[] DUBLED_COMMAND_ID = new String[]{"-p",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "DoubledCommandId.txt", "-c",
        "src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "config" + File.separator + "bspCredentials.txt"};

    @Before
    public void createConfigHandler() {
        ConfigHandler.setConfigHandler(null);
    }

    @Test
    public void checkParsingError() {
        ConfigHandler.init(ARGS_Wrong_Formatted);
        ReadException exception = assertThrows(ReadException.class,
            () -> new CommandsBuilder().buildCommands());
        assertEquals(
            "An error have been occurred while reading your command file. Please check if this file is a valid .xml file",
            exception.getMessage());
    }

    @Test
    public void checkFileNotFoundError() {
        ConfigHandler.init(ARGS_MISSING_COMMANDS);
        ReadException exception = assertThrows(ReadException.class,
            () -> new CommandsBuilder().buildCommands());
        assertEquals(
            "A problem with your named file have occurred. Please if check " + new File("doesNotExist.xml").getAbsolutePath() + " exist",
            exception.getMessage());
    }

    @Test
    public void doubledCommandIdDefinedTest() {
        ConfigHandler.init(DUBLED_COMMAND_ID);
        CommandException exception = assertThrows(CommandException.class, () -> new CommandsBuilder().buildCommands());
        assertEquals("The predefined ID \"thisShouldOccureAnError\" occurs twice", exception.getMessage());
    }
}
