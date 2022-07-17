package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.BaseDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.DirectoryEntry;
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

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.List;

public class SafeModifyDirEntryExecution extends ExecutionBase {

  private Logger LOG = LoggerFactory.getLogger(SafeModifyDirEntryExecution.class);
  private static final int FIRST_INDEX = 0;

  public SafeModifyDirEntryExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.SMOD_DIR_ENTRY);
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
      String telematikId = command.getUserCertificate().get(FIRST_INDEX).getTelematikID();
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

  private CommandType getSaveCommand(CommandType command, GemApiClient apiClient)
      throws ApiException, DatatypeConfigurationException {
    ApiResponse<List<DirectoryEntry>> response = new GemDirectoryEntryAdministrationApi(apiClient)
        .readDirectoryEntryWithHttpInfo(command.getDn().getUid(), null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null);
    CommandType saveCommand = Transformer.getCommandTypeFromDirectoryEntry(
        response.getData().get(0));

    saveCommand.setCommandId(command.getCommandId());
    saveCommand.setName(command.getName());

    if (command.getDn() != null) {
      saveCommand.setDn(command.getDn());
    }
    if (StringUtils.isNotBlank(command.getGivenName())) {
      saveCommand.setGivenName(command.getGivenName());
    }
    if (StringUtils.isNotBlank(command.getSn())) {
      saveCommand.setSn(command.getSn());
    }
    if (StringUtils.isNotBlank(command.getCn())) {
      saveCommand.setCn(command.getCn());
    }
    if (StringUtils.isNotBlank(command.getDisplayName())) {
      saveCommand.setDisplayName(command.getDisplayName());
    }
    if (StringUtils.isNotBlank(command.getStreetAddress())) {
      saveCommand.setStreetAddress(command.getStreetAddress());
    }
    if (StringUtils.isNotBlank(command.getPostalCode())) {
      saveCommand.setPostalCode(command.getPostalCode());
    }
    if (StringUtils.isNotBlank(command.getCountryCode())) {
      saveCommand.setCountryCode(command.getCountryCode());
    }
    if (StringUtils.isNotBlank(command.getLocalityName())) {
      saveCommand.setLocalityName(command.getLocalityName());
    }
    if (StringUtils.isNotBlank(command.getStateOrProvinceName())) {
      saveCommand.setStateOrProvinceName(command.getStateOrProvinceName());
    }
    if (StringUtils.isNotBlank(command.getTitle())) {
      saveCommand.setTitle(command.getTitle());
    }
    if (StringUtils.isNotBlank(command.getOrganization())) {
      saveCommand.setOrganization(command.getOrganization());
    }
    if (StringUtils.isNotBlank(command.getOtherName())) {
      saveCommand.setOtherName(command.getOtherName());
    }
    if (!command.getSpecialization().isEmpty()) {
      while (!saveCommand.getSpecialization().isEmpty()) {
        saveCommand.getSpecialization().remove(FIRST_INDEX);
      }
      saveCommand.getSpecialization().addAll(command.getSpecialization());
    }
    if (!command.getDomainID().isEmpty()) {
      while (!saveCommand.getDomainID().isEmpty()) {
        saveCommand.getDomainID().remove(FIRST_INDEX);
      }
      saveCommand.getDomainID().addAll(command.getDomainID());
    }
    if (!command.getHolder().isEmpty()) {
      while (!saveCommand.getHolder().isEmpty()) {
        saveCommand.getHolder().remove(FIRST_INDEX);
      }
      saveCommand.getHolder().addAll(command.getHolder());
    }
    if (StringUtils.isNotBlank(command.getMaxKOMLEadr())) {
      saveCommand.setMaxKOMLEadr(command.getMaxKOMLEadr());
    }
    if (!command.getProfessionOID().isEmpty()) {
      saveCommand.getProfessionOID().addAll(command.getProfessionOID());
    }
    return saveCommand;
  }

  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException, DatatypeConfigurationException {

    if (!isEntryPresent(command, apiClient)) {
      return doAdd(command, apiClient);
    }
    CommandType safeCommand = getSaveCommand(command, apiClient);
    BaseDirectoryEntry baseDirectoryEntry = Transformer.getBaseDirectoryEntryFromCommandType(
        safeCommand);
    if (baseDirectoryEntry.getDn() == null) {
      baseDirectoryEntry.setDn(new DistinguishedName());
    }
    if (StringUtils.isBlank(baseDirectoryEntry.getDn().getUid())) {
      baseDirectoryEntry.getDn().setUid(getUid(command, apiClient));
    }
    ApiResponse<DistinguishedName> response = new GemDirectoryEntryAdministrationApi(apiClient)
        .modifyDirectoryEntryWithHttpInfo(baseDirectoryEntry.getDn().getUid(), baseDirectoryEntry);
    if (response.getStatusCode() == HttpStatus.SC_OK) {
      return (new ExecutionResult(
          "Safe-modify directory entry execution successful operated\n" + response.getData(), true,
          response.getStatusCode()));
    } else {
      throw new ApiException(
          "Modify directory entry execution failed. Response status was: "
              + response.getStatusCode() + "\n"
              + Transformer.getBaseDirectoryEntryFromCommandType(command));
    }

  }

  private ExecutionResult doAdd(CommandType command, GemApiClient apiClient) throws ApiException {
    LOG.debug("Entry not present at VZD. Will proceed with add directory entry command");
    StringBuffer sb = new StringBuffer(
        "\nEntry is already present in VZD. Will Proceed with modify directory entry command\n");
    ExecutionResult addExecutionResult = ExecutionCollection.getInstance().getAddDirEntryExecution()
        .executeCommand(command, apiClient);
    sb.append(addExecutionResult.getMessage() + "\n +++ proceeded as modify +++");
    return new ExecutionResult(sb.toString(), addExecutionResult.getResult(),
        addExecutionResult.getHttpStatusCode());
  }
}