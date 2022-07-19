/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.CreateDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.DistinguishedName;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemDirectoryEntryAdministrationApi;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollection;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.UserCertificateType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific execution for Command "AddDirectoryEntry"
 */
public class AddDirEntryExecution extends ExecutionBase {

  private Logger LOG = LoggerFactory.getLogger(AddDirEntryExecution.class);

  public AddDirEntryExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.ADD_DIR_ENTRY);
  }

  @Override
  public boolean checkValidation(CommandType command) {
    if (StringUtils.isNotBlank(findTelematiId(command))) {
      return true;
    } else if (command.getUserCertificate().isEmpty()) {
      return false;
    }
    boolean check = true;
    for (UserCertificateType userCertificateType : command.getUserCertificate()) {
      String telematikId = userCertificateType.getTelematikID();
      String userCertificate = userCertificateType.getUserCertificate();
      if ((StringUtils.isBlank(telematikId) && StringUtils.isBlank(userCertificate))) {
        check = false;
      }
    }
    if (!check) {
      LOG.error("Missing argument -> telematikId or userCertificate in every entry have to be set."
          + command.getName() + "\n"
          + Transformer.getBaseDirectoryEntryFromCommandType(command));
    }
    return check;
  }

  @Override
  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException {
    if (isEntryPresent(command, apiClient)) {
      return doModify(command, apiClient);
    }
    return doAdd(command, apiClient);
  }

  private ExecutionResult doAdd(CommandType command, GemApiClient apiClient) throws ApiException {
    CreateDirectoryEntry createDirectoryEntry = Transformer.getCreateDirectoryEntry(command);
    ApiResponse<DistinguishedName> response = new GemDirectoryEntryAdministrationApi(apiClient)
        .addDirectoryEntryWithHttpInfo(createDirectoryEntry);
    if (response.getStatusCode() == HttpStatus.SC_CREATED) {
      return new ExecutionResult(
          "\nAdd directory entry execution successful operated\n" + response.getData(), true,
          response.getStatusCode());
    }
    throw new ApiException(response.getStatusCode(),
        "Add directory entry execution failed. Response status was: "
            + response.getStatusCode() + "\n"
            + Transformer.getCreateDirectoryEntry(command));
  }


  private ExecutionResult doModify(CommandType command, GemApiClient apiClient)
      throws ApiException {
    StringBuffer sb = new StringBuffer();
    sb.append(
        "\nEntry is already present in VZD. Will proceed with modify directory entry command\n");
    ExecutionResult modExecutionResult = ExecutionCollection.getInstance().getModifyDirEntry()
        .executeCommand(command, apiClient);
    sb.append(modExecutionResult.getMessage() + "\n +++ proceeded as modify +++");
    return new ExecutionResult(sb.toString(), modExecutionResult.getResult(),
        modExecutionResult.getHttpStatusCode());
  }

  @Override
  public boolean postCheck() {
    try {
      super.postCheck();
      return true;
    } catch (Exception ex) {
      LOG.error(ex.getMessage());
    }
    return false;
  }
}
