/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command;

import de.gematik.ti.epa.vzd.gem.command.parser.CommandParser;
import de.gematik.ti.epa.vzd.gem.command.parser.ReadListParser;
import de.gematik.ti.epa.vzd.gem.command.parser.XmlExecutionParser;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import generated.CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class is responsible for reading the commands out of the .xml File. Before the commands can
 * be build the ConfigHandler have to be initialized
 */
public class CommandsBuilder {

  private Logger LOG = LoggerFactory.getLogger(CommandsBuilder.class);
  private CommandParser parser;

  public CommandsBuilder() {
    String path = ConfigHandler.getInstance().getCommandsPath();
    parser = getParser(path);
  }

  /**
   * Reads the given command file and creates a list of commands to execute
   *
   * @return CommandListType - List of all commands to execute
   */
  public List<CommandType> buildCommands() {
    return parser.buildCommands();
  }

  private CommandParser getParser(String path) {
    if (path.endsWith(".xml")) {
      LOG.debug("Get commands from .xml input");
      return new XmlExecutionParser();
    } else if (path.endsWith(".txt")) {
      LOG.debug("Get commands from list from .txt input");
      return new ReadListParser();
    }
    throw new CommandException("The command file you have provided is not valid: " + path);
  }

}
