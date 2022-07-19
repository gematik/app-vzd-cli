/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.BaseDirectoryEntry;
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
 * Specific execution for Command "ModifyDirectoryEntry"
 */
public class ModifyDirEntryExecution extends ExecutionBase {

  private Logger LOG = LoggerFactory.getLogger(ModifyDirEntryExecution.class);

  public ModifyDirEntryExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.MOD_DIR_ENTRY);
  }

  @Override
  public boolean checkValidation(CommandType command) {
    if (command.getDn() != null) {
      if (StringUtils.isBlank(command.getDn().getUid())) {
        LOG.error(
            "Missing argument -> uid for command " + command.getName() + "\n" + Transformer
                .getBaseDirectoryEntryFromCommandType(command));
        return false;
      }
      return true;
    }
    if (!command.getUserCertificate().isEmpty()) {
      String telematikId = command.getUserCertificate().get(0).getTelematikID();
      for (UserCertificateType userCertificateType : command.getUserCertificate()) {
        if (!telematikId.equals(userCertificateType.getTelematikID())) {
          return false;
        }
      }
      return true;
    }
    LOG.error("Missing element \"dn\" " + command.getName() + "\n" + Transformer
        .getBaseDirectoryEntryFromCommandType(command));
    return false;
  }

  @Override
  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException {
    if (!isEntryPresent(command, apiClient)) {
      return doAdd(command, apiClient);
    } else {
      return doModify(command, apiClient);
    }
  }

  private ExecutionResult doModify(CommandType command, GemApiClient apiClient)
      throws ApiException {
    ApiResponse<DistinguishedName> response;
    BaseDirectoryEntry baseDirectoryEntry = Transformer
        .getBaseDirectoryEntryFromCommandType(command);
    if (baseDirectoryEntry.getDn() == null) {
      baseDirectoryEntry.setDn(new DistinguishedName());
    }
    if (StringUtils.isBlank(baseDirectoryEntry.getDn().getUid())) {
      baseDirectoryEntry.getDn().setUid(getUid(command, apiClient));
    }
    response = new GemDirectoryEntryAdministrationApi(apiClient)
        .modifyDirectoryEntryWithHttpInfo(baseDirectoryEntry.getDn().getUid(), baseDirectoryEntry);
    if (response.getStatusCode() == HttpStatus.SC_OK) {
      return (new ExecutionResult(
          "Modify directory entry execution successful operated\n" + response.getData(), true,
          response.getStatusCode()));
    } else {
      throw new ApiException(
          "Modify directory entry execution failed. Response status was: "
              + response.getStatusCode() + "\n"
              + Transformer.getBaseDirectoryEntryFromCommandType(command));
    }
  }

  private ExecutionResult doAdd(CommandType command, GemApiClient apiClient) throws ApiException {
    LOG.debug("Entry not present in VZD. Will proceed with add directory entry command");
    StringBuffer sb = new StringBuffer(
        "\nEntry is not present in VZD. Will proceed with add directory entry command\n");
    ExecutionResult addExecutionResult = ExecutionCollection.getInstance().getAddDirEntryExecution()
        .executeCommand(command, apiClient);
    sb.append(addExecutionResult.getMessage() + "\n +++ proceeded as modify +++");
    return new ExecutionResult(sb.toString(), addExecutionResult.getResult(),
        addExecutionResult.getHttpStatusCode());
  }
}
