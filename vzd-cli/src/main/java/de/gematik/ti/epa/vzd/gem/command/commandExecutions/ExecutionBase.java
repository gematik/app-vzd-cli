/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.UserCertificate;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemCertificateAdministrationApi;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.BaseExecutionResult;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.exceptions.GemClientException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import de.gematik.ti.epa.vzd.gem.utils.LogHelper;
import generated.CommandType;
import generated.DistinguishedNameType;
import generated.UserCertificateType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Represents the base for every specific execution
 */
public abstract class ExecutionBase implements Callable<BaseExecutionResult> {

  private Logger LOG = LoggerFactory.getLogger(ExecutionBase.class);
  protected static final int FIRST_INDEX = 0;

  protected final IConnectionPool connectionPool;
  protected CommandNamesEnum execCommand;
  protected List<CommandType> commands;

  public ExecutionBase(IConnectionPool connectionPool, CommandNamesEnum cmd) {
    this.connectionPool = connectionPool;
    this.execCommand = cmd;
    this.commands = new ArrayList<>();
  }

  /**
   * Every single executor validates their commands and log the missing or wrong values
   *
   * @param command
   * @return
   */
  public abstract boolean checkValidation(CommandType command);

  /**
   * Creates Callable to execute one command
   *
   * @param command
   * @return
   */
  protected Callable<Boolean> createCallable(CommandType command) {
    return new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("\n--- Command  " + command.getCommandId() + " ---\n");
        if (ConfigHandler.getInstance().isLogCommands()) {
          sb.append("=== Executing command ===\n" + Transformer.getCreateDirectoryEntry(command)
              + "\n================\n");
        }
        ExecutionResult executionResult;
        try (GemApiClient apiClient = connectionPool.getConnection()) {
          apiClient.validateToken();
          executionResult = executeCommand(command, apiClient);
          sb.append(executionResult.getMessage());
          LogHelper.logCommand(execCommand, command, executionResult.getResult());
        } catch (ApiException ex) {
          sb.append(StringUtils.isBlank(ex.getMessage()) ?
              "Something went wrong with " + Transformer.getBaseDirectoryEntryFromCommandType(
                  command) : ex.getMessage());
          sb.append(
              "\n--- Command  " + command.getCommandId() + " end (Http status was: " + ex.getCode()
                  + ")---");
          LOG.error(sb.toString());
          LogHelper.logCommand(execCommand, command, false);
          return false;
        } catch (Throwable ex) {
          LOG.error("",ex);
          sb.append("Something went wrong with " + Transformer.getBaseDirectoryEntryFromCommandType(
              command));
          sb.append("\n--- Command  " + command.getCommandId() + " end");
          LOG.error(sb.toString());
          LogHelper.logCommand(execCommand, command, false);
          return false;
        }
        sb.append("\n--- Command  " + command.getCommandId() + " end (Http status was: "
            + executionResult.getHttpStatusCode() + ") ---");
        LOG.debug(sb.toString());
        return executionResult.getResult();
      }
    };

  }

  /**
   * Function that execute one command and returns the result
   *
   * @param command
   * @param apiClient
   * @return
   */
  protected abstract ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException, DatatypeConfigurationException;

  /**
   * Checks if the Executor can progress the named Command
   *
   * @param cmd
   * @return
   */
  public boolean canHandleCommand(CommandNamesEnum cmd) {
    return this.execCommand.equals(cmd);
  }

  /**
   * Checks the given command for validation and adds it to the queue of commands to execute
   *
   * @param command
   * @return
   */
  public boolean preCheck(CommandType command) {
    try {
      if (!checkValidation(command)) {
        throw new CommandException(
            "Command invalid. Please check " + command.getName());
      }
      commands.add(command);
      return true;
    } catch (Exception ex) {
      LOG.error(ex.getMessage());
      return false;
    }
  }

  /**
   * Gets the name of the specific executor for logging
   *
   * @param executor
   * @return
   */
  protected String extractExecutorName(ExecutionBase executor) {
    String[] splitClass = executor.getClass().getName().split("\\.");
    return splitClass[splitClass.length - 1];
  }

  /**
   * Checks if the execution was successful and logs the result
   *
   * @return
   */
  public boolean postCheck() {
    try {
      return true;
    } catch (Exception ex) {
      LOG.error(ex.getMessage());
    }
    return false;
  }

  /**
   * This function proceed a read command, to check if a entry without cert is already present
   *
   * @param command
   * @return
   */
  protected boolean isEntryPresent(CommandType command, GemApiClient apiClient)
      throws ApiException {
    if (command.getDn() != null) {
      CommandType searchCommand = new CommandType();
      DistinguishedNameType dn = new DistinguishedNameType();
      dn.setUid(getUid(command, apiClient));
      searchCommand.setDn(dn);
      try {
        return ExecutionCollection.getInstance().getReadDirEntryExecution()
            .executeCommand(searchCommand, apiClient).getHttpStatusCode() == HttpStatus.SC_OK ? true
            : false;
      } catch (ApiException ex) {
        if (ex.getCode() == 0) {
          throw new GemClientException(
              "The server you address is probably not reachable at the moment");
        }
        LOG.error(ex.getMessage());
        return false;
      }
    } else {
      return serachByTelematikId(command, apiClient);
    }
  }

  /**
   * This function proceed a read command, to check if a entry without cert is already present
   *
   * @param command
   * @return
   */
  private boolean serachByTelematikId(CommandType command, GemApiClient apiClient) {
    String telematikId = findTelematiId(command);
    try {
      CommandType searchCommand = new CommandType();
      searchCommand.getUserCertificate().add(new UserCertificateType());
      searchCommand.getUserCertificate().get(FIRST_INDEX)
          .setTelematikID(telematikId);
      ApiResponse<List<UserCertificate>> response = ExecutionCollection
          .getInstance().getReadDirEntryCertExecution().getResult(searchCommand, apiClient);
      if (!Objects.nonNull(command.getDn())) {
        command.setDn(new DistinguishedNameType());
      }
      command.getDn().setUid(response.getData().get(FIRST_INDEX).getDn().getUid());
      return response.getStatusCode() == HttpStatus.SC_OK && response.getData().size() == 1 ? true
          : false;
    } catch (ApiException ex) {
      if (ex.getCode() == 0) {
        throw new GemClientException(
            "The server you address is probably not reachable at the moment");
      }
      return false;
    }
  }

  protected String findTelematiId(CommandType command) {
    Set<String> telematikIds = command.getUserCertificate().stream().map(e -> e.getTelematikID())
        .collect(Collectors.toSet());
    telematikIds.add(command.getTelematikID());
    telematikIds.remove(null);

    if (telematikIds.size() > 1) {
      throw new CommandException(
          String.format("At least two different TelematikIds found in command -> %s",
              telematikIds));
    }
    return telematikIds.isEmpty() ? null : telematikIds.iterator().next();
  }

  public boolean searchByUserCertificate(CommandType command, GemApiClient apiClient) {
    ApiResponse<List<UserCertificate>> response;
    try {
      response = ExecutionCollection.getInstance().getReadDirEntryCertExecution()
          .getResult(command, apiClient);
    } catch (ApiException ex) {
      if (ex.getCode() == 0) {
        throw new GemClientException(
            "The server you address is probably not reachable at the moment");
      }
      LOG.error(ex.getMessage());
      return false;
    }
    if (response.getData().size() == command.getUserCertificate().size()) {
      return true;
    }
    return false;
  }

  public String getUidByTelematikId(String telematikId, GemApiClient apiClient)
      throws ApiException {
    ApiResponse<List<UserCertificate>> response = null;
    try {
      response = new GemCertificateAdministrationApi(apiClient)
          .readDirectoryCertificatesWithHttpInfo(null, null, null,
              telematikId, null, null);
      if (!response.getData().isEmpty()) {
        return response.getData().get(FIRST_INDEX).getDn().getUid();
      }
    } catch (ApiException ex) {
      if (ex.getCode() == 0) {
        throw new GemClientException(
            "The server you address is probably not reachable at the moment");
      }
      if (ex.getCode() == 404) {
        return null;
      }
    }
    throw new ApiException("Problem occurred while finding uid via telematikId");
  }


  @Override
  public BaseExecutionResult call() {
    LOG.debug("CALL " + extractExecutorName(this));

    if (commands.size() == 0) {
      LOG.debug("CALL END " + extractExecutorName(this));
      return new BaseExecutionResult(extractExecutorName(this), true);
    }
    List<Callable<Boolean>> callables = new ArrayList<>();
    ExecutorService executorService = Executors.newFixedThreadPool(
        ConfigHandler.getInstance().getMaxParaExecutionsPerExecutor());
    for (CommandType command : commands) {
      callables.add(createCallable(command));
    }
    try {
      List<Future<Boolean>> futures = executorService.invokeAll(callables);
      Optional<Future<Boolean>> first =
          futures.stream()
              .filter(
                  booleanFuture -> {
                    try {
                      return !booleanFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                      Thread.currentThread().interrupt();
                      return false;
                    }
                  })
              .findFirst();
      if (first.isPresent()) {
        return new BaseExecutionResult(extractExecutorName(this), false);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new BaseExecutionResult(extractExecutorName(this), false);
    } finally {
      LOG.debug("CALL END " + extractExecutorName(this));
      executorService.shutdown();
    }

    return new BaseExecutionResult(extractExecutorName(this), true);
  }


  protected String getUid(CommandType command, GemApiClient apiClient) throws ApiException {
    String uidFromTelematikID = null;
    String uidFromCommand = null;

    if (command.getDn() != null) {
      uidFromCommand = command.getDn().getUid();
    }
    if (!command.getUserCertificate().isEmpty()) {
      if (command.getUserCertificate().get(FIRST_INDEX).getDn() != null && StringUtils.isBlank(
          uidFromCommand)) {
        uidFromCommand = command.getUserCertificate().get(FIRST_INDEX).getDn().getUid();
      }
      if (StringUtils.isNotBlank(command.getUserCertificate().get(FIRST_INDEX).getTelematikID())) {
        uidFromTelematikID = getUidByTelematikId(
            command.getUserCertificate().get(FIRST_INDEX).getTelematikID(), apiClient);
      }
    }
    if (StringUtils.isNotBlank(uidFromCommand) && StringUtils.isNotBlank(uidFromTelematikID)) {
      if (!uidFromCommand.equals(uidFromTelematikID)) {
        throw new ApiException(
            "UID delivered by TelematikId does not match the UID in command file");
      }
    }
    return StringUtils.isBlank(uidFromCommand) ? uidFromTelematikID : uidFromCommand;
  }

  public void reset() {
    this.commands = new ArrayList<>();
  }
}
