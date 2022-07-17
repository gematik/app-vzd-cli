/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.DirectoryEntry;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemDirectoryEntryAdministrationApi;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import de.gematik.ti.epa.vzd.gem.utils.GemStringUtils;
import generated.CommandType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Specific execution for Command "ReadDirectoryEntry"
 */
public class ReadDirEntryExecution extends ExecutionBase {

  private Logger LOG = LoggerFactory.getLogger(ReadDirEntryExecution.class);

  public ReadDirEntryExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.READ_DIR_ENTRY);
  }

  public ReadDirEntryExecution(IConnectionPool connectionPool, CommandNamesEnum commandNamesEnum) {
    super(connectionPool, commandNamesEnum);
  }

  @Override
  public boolean checkValidation(CommandType command) {
    List<String> params = new ArrayList<>();
    if (command.getDn() != null) {
      params.add(command.getDn().getUid());
    }
    params.add(command.getGivenName());
    params.add(command.getSn());
    params.add(command.getCn());
    params.add(command.getDisplayName());
    params.add(command.getStreetAddress());
    params.add(command.getPostalCode());
    params.add(command.getCountryCode());
    params.add(command.getLocalityName());
    params.add(command.getStateOrProvinceName());
    params.add(command.getTitle());
    params.add(command.getOrganization());
    params.add(command.getOtherName());
    try {
      params.add(findTelematiId(command));
    } catch (CommandException e) {
      LOG.error(e.getMessage() + "\n" + command.getName() + "\n"
          + Transformer.getBaseDirectoryEntryFromCommandType(command));
      throw e;
    }
    params.add(command.getTelematikIDSubStr());
    params.add(GemStringUtils.listToString(command.getSpecialization()));
    params.add(GemStringUtils.listToString(command.getDomainID()));
    params.add(GemStringUtils.listToString(command.getHolder()));
    params.add(command.getPersonalEntry());
    params.add(command.getDataFromAuthority());
    if (!command.getProfessionOID().isEmpty() || !command.getEntryType().isEmpty()) {
      params.add("ProfessionOID or Entrytype delivered");
    }
    Optional<Boolean> check = params.stream()
        .map(StringUtils::isBlank)
        .filter(b -> !b)
        .findAny();
    if (check.isEmpty()) {
      LOG.error(
          "Missing argument -> The given command have no argument to search for or given different TelematikIds delivered "
              + command.getName() + "\n" + Transformer.getBaseDirectoryEntryFromCommandType(
              command));
      return false;
    }
    return true;
  }

  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException {
    String uid = null;
    if (command.getDn() != null) {
      uid = command.getDn().getUid();
    }
    String givenName = command.getGivenName();
    String sn = command.getSn();
    String cn = command.getCn();
    String displayName = command.getDisplayName();
    String streetAddress = command.getStreetAddress();
    String postalCode = command.getPostalCode();
    String countryCode = command.getCountryCode();
    String localityName = command.getLocalityName();
    String stateOrProvinceName = command.getStateOrProvinceName();
    String title = command.getTitle();
    String organization = command.getOrganization();
    String otherName = command.getOtherName();
    String telematikID = findTelematiId(command);
    String telematikIDSubStr = command.getTelematikIDSubStr();
    String specialization = GemStringUtils.listToString(command.getSpecialization());
    String domainID = GemStringUtils.listToString(command.getDomainID());
    String holder = GemStringUtils.listToString(command.getHolder());
    String personalEntry = command.getPersonalEntry();
    String dataFromAuthority = command.getDataFromAuthority();
    String professionOID = GemStringUtils.listToString(command.getProfessionOID());
    String entryType = GemStringUtils.listToString(command.getEntryType());
    boolean baseEntryOnly = false;
    if (command.isBaseEntryOnly() != null) {
      baseEntryOnly = command.isBaseEntryOnly();
    }
    ApiResponse<List<DirectoryEntry>> response;
    try {
      response = new GemDirectoryEntryAdministrationApi(apiClient)
          .readDirectoryEntryWithHttpInfo(uid, givenName, sn, cn, displayName, streetAddress,
              postalCode, countryCode, localityName,
              stateOrProvinceName, title, organization, otherName, telematikID, telematikIDSubStr,
              specialization, domainID, holder,
              personalEntry, dataFromAuthority, professionOID, entryType, baseEntryOnly);
    } catch (ApiException ex) {
      throw new ApiException(ex.getCode(),
          "Entry could not be found: \n" + Transformer.getBaseDirectoryEntryFromCommandType(
              command));
    }
    StringBuffer sb = new StringBuffer();
    sb.append(String.format(":: Entries found: %s ::", response.getData().size()));
    response.getData()
        .forEach(directoryEntry -> sb.append("\nEntry found: ").append(directoryEntry));
    return new ExecutionResult(sb.toString(), true, response.getStatusCode());
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

