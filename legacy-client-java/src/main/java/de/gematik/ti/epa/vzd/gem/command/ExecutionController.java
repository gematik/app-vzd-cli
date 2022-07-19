/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command;

import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ExecutionBase;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import generated.CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * This class handles the execution process. It orders the commands to the specific executors where
 * they belong and calls the
 */
public class ExecutionController {

  private Logger LOG = LoggerFactory.getLogger(ExecutionController.class);


  /**
   * Order the commands to the specific executions where they belong and than call the execution
   * functions of the specific executors
   *
   * @param commandList
   */
  public void execute(List<CommandType> commandList) {
    Map<String, Boolean> report = new HashMap<>();
    loadCommands(commandList);
    LOG.debug("Execution -> Run executions");
    Optional<Boolean> correctExecution;

    ExecutorService executorService = Executors.newFixedThreadPool(
        ConfigHandler.getInstance().getMaxParallelExecutor());

    try {
      correctExecution = executorService
          .invokeAll(ExecutionCollection.getInstance().getExecutors())
          .parallelStream()
          .map(
              executor -> {
                try {
                  report.put(executor.get().getName(), executor.get().getResult());
                  if (!executor.get().getResult()) {
                    LOG.error("Error while execute commands in " + executor.get().getName());
                  } else {
                    LOG.debug(
                        "All commands of " + executor.get().getName() + " operated correctly");
                  }
                  return executor.get().getResult();
                } catch (Exception e) {
                  LOG.error("Something went wrong. " + e.getMessage());
                }
                return false;
              })
          .filter(b -> !b)
          .findAny();
    } catch (InterruptedException e) {
      correctExecution = Optional.of(false);
      Thread.currentThread().interrupt();
    } finally {
      executorService.shutdown();
    }
    logReport(report, correctExecution);

  }

  private void logReport(Map<String, Boolean> report, Optional<Boolean> correctExecution) {
    Iterator<String> keys = report.keySet().iterator();
    while (keys.hasNext()) {
      String key = keys.next();
      LOG.info(report.get(key) + " <-- All executions for " + key + " run correctly");
    }
    String path = System.getProperties().getProperty("l4j.logDir") == null ?
        System.getProperties().getProperty("java.io.tmpdir") + "logs"
        : System.getProperties().getProperty("l4j.logDir");
    LOG.info("Execution -> All executions done" + (correctExecution.isEmpty() ? " correctly"
        : ". Some commands failed. Please look at the logfile at " + path));
  }

  private void loadCommands(List<CommandType> commandList) {
    LOG.debug("Execution -> Precheck started");
    ExecutionCollection.getInstance().resetExecutors();
    boolean commandError = false;
    for (CommandType command : commandList) {
      boolean unknownCommand = true;
      for (ExecutionBase specificExecutor : ExecutionCollection.getInstance().getExecutors()) {
        if (specificExecutor.canHandleCommand(CommandNamesEnum.getEntry(command.getName()))) {
          unknownCommand = false;
          if (!specificExecutor.preCheck(command)) {
            commandError = true;
          }
        }
      }
      if (unknownCommand) {
        LOG.error("Unknown command " + command.getName() + "\n" + Transformer
            .getBaseDirectoryEntryFromCommandType(command));
        commandError = true;
      }
    }
    if (commandError) {
      throw new CommandException("Commands not executed, preCheck failed!");
    }
    LOG.debug("Execution -> Precheck successful");
  }
}

