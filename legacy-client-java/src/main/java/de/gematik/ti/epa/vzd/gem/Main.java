/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem;

import de.gematik.ti.epa.vzd.gem.command.CommandsBuilder;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.command.ExecutionController;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import de.gematik.ti.epa.vzd.gem.utils.GemStringUtils;
import generated.CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(final String[] args) {
    start(args, null);
  }
  public static void start(final String[] args, @Nullable ConfigHandler.PreConfig preConfig) {
    LOG.info("VZD-Client started");
    LOG.info(GemStringUtils.getPic());
    ConfigHandler configHandler = ConfigHandler.init(args, preConfig);
    if (configHandler.isGetVersion()) {
      LOG.info("You are currently using version " + configHandler.getClientVersion());
      return;
    }
    start();
  }

  private static void start() {
    ConfigHandler configHandler = ConfigHandler.getInstance();
    List<CommandType> commands = new CommandsBuilder().buildCommands();
    printClientInfo(configHandler, commands.size());

    configHandler.adjustConnectionCount(commands);
    ExecutionCollection.init(
        ConnectionPool.createConnectionPool(configHandler.getConnectionCount()));

    if (!ConfigHandler.getInstance().isChunked()) {
      new ExecutionController().execute(commands);
      return;
    }

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SS");
    System.setProperty("l4j.logDir",
        System.getProperty("l4j.logDir") + "/VZD-Client_" + LocalDateTime.now()
            .format(dateTimeFormatter));
    startChunked(commands);
  }

  private static void printClientInfo(ConfigHandler configHandler, int commands) {
    LOG.debug("============ Execution parameter ============");
    LOG.debug("Server: " + configHandler.getBasePath());
    LOG.debug("OAuth Server: " + configHandler.getRetryingOAuthPath());
    LOG.debug("Command data: " + configHandler.getCommandsPath());
    LOG.debug("Config path: " + configHandler.getConfigPath());
    LOG.debug("Commands in progress: " + commands);
    LOG.debug("VZD-Client version: " + configHandler.getClientVersion());
    LOG.debug("=============================================");
  }

  private static void startChunked(List<CommandType> commands) {
    ConfigHandler configHandler = ConfigHandler.getInstance();
    long from = configHandler.getChunkFrom();
    long till = configHandler.getChunkTill();
    List<List<CommandType>> chunkedList = chunkList(commands);
    int count = 1;
    for (List<CommandType> chunk : chunkedList) {
      if (count >= from && count + chunk.size() - 1 <= till) {
        System.setProperty("l4j.logFileName", count + "-" + (count + chunk.size() - 1));
        //((LoggerContext) LogManager.getContext(false)).reconfigure();
        new ExecutionController().execute(chunk);
      }
      count += chunk.size();
    }
  }

  private static List<List<CommandType>> chunkList(List<CommandType> commands) {
    int chunkSize = ConfigHandler.getInstance().getChunkSize();
    int count = 0;
    List<List<CommandType>> chunkedList = new ArrayList<>();
    while (count < commands.size()) {
      int end = count + chunkSize > commands.size() ? commands.size() : count + chunkSize;
      chunkedList.add(commands.subList(count, end));
      count += chunkSize;
    }
    return chunkedList;
  }

  // <editor-fold desc="Private Constructor">
  private Main() {
    super();
  }
  // </editor-fold>
}
