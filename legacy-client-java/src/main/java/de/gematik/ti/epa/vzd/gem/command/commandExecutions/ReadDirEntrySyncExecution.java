package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.DirectoryEntries;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemDirectoryEntrySynchronizationApi;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import de.gematik.ti.epa.vzd.gem.utils.GemStringUtils;
import generated.CommandType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

public class ReadDirEntrySyncExecution extends ReadDirEntryExecution {

  public ReadDirEntrySyncExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.READ_DIR_ENTRY_SYNC);
  }

  public ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
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
    String owner = GemStringUtils.listToString(command.getHolder());
    String personalEntry = command.getPersonalEntry();
    String dataFromAuthority = command.getDataFromAuthority();
    boolean baseEntryOnly = false;
    if (command.isBaseEntryOnly() != null) {
      baseEntryOnly = command.isBaseEntryOnly();
    }

    if (!command.getUserCertificate().isEmpty()) {
      if (StringUtils.isBlank(uid) && StringUtils.isNotBlank(
          command.getUserCertificate().get(0).getTelematikID())) {
        throw new ApiException(
            "No entry present for telematikID: " + command.getUserCertificate().get(0)
                .getTelematikID());
      }
    }
    ApiResponse<DirectoryEntries> response =
        new GemDirectoryEntrySynchronizationApi(apiClient)
            .readDirectoryEntryForSyncWithHttpInfo(uid, givenName, sn, cn, displayName,
                streetAddress, postalCode, countryCode, localityName,
                stateOrProvinceName, title, organization, otherName, telematikID, telematikIDSubStr,
                specialization, domainID, owner,
                personalEntry, dataFromAuthority, baseEntryOnly);

    if (response.getStatusCode() == HttpStatus.SC_OK) {
      StringBuffer sb = new StringBuffer();
      sb.append(String.format(":: Entries found: %s ::", response.getData().size()));
      response.getData()
          .forEach(directoryEntry -> sb.append("\nEntry found: ").append(directoryEntry));
      return new ExecutionResult(sb.toString(), true, response.getStatusCode());
    } else {
      throw new ApiException(
          "Read directory entry sync execution failed. Response status was: "
              + response.getStatusCode() + "\n"
              + Transformer.getBaseDirectoryEntryFromCommandType(command));
    }
  }

}
