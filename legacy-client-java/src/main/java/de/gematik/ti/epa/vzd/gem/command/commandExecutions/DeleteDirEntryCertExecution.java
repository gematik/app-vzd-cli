/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.CreateDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.UserCertificate;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemCertificateAdministrationApi;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.UserCertificateType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific execution for Command "DeleteDirectoryEntryCertificate"
 */
public class DeleteDirEntryCertExecution extends ExecutionBase {

  private Logger LOG = LoggerFactory.getLogger(AddDirEntryExecution.class);


  public DeleteDirEntryCertExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.DEL_DIR_CERT);
  }


  @Override
  public boolean checkValidation(CommandType command) {

    for (UserCertificateType cert : command.getUserCertificate()) {
      if (cert.getDn() == null) {
        LOG.error("Every certificate should have a dn");
        return false;
      }
      if (StringUtils.isBlank(cert.getDn().getCn())) {
        LOG.error("Every certificate should have a cn");
        return false;
      }
    }
    try {
      getUid(command);
    } catch (CommandException ex) {
      return false;
    }
    return true;
  }

  private String getUid(CommandType command) {
    String uid = null;
    if (command.getDn() != null) {
      if (StringUtils.isNotBlank(command.getDn().getUid())) {
        uid = command.getDn().getUid();
      }
    }
    for (UserCertificateType cert : command.getUserCertificate()) {
      if (StringUtils.isBlank(uid)) {
        uid = cert.getDn().getUid();
      } else {
        if (!uid.equals(cert.getDn().getUid())) {
          LOG.error("Different uid's delivered");
          throw new CommandException("Different uid's delivered");
        }
      }
    }
    if (StringUtils.isBlank(uid)) {
      LOG.error("Different uid's delivered in {}", command.getCommandId());
      throw new CommandException("Different uid's delivered");
    }
    return uid;
  }

  @Override
  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException {
    StringBuffer sb = new StringBuffer();
    ApiResponse<Void> response = null;
    boolean runSucsessfull = true;
    int errorCode = 0;
    String uid = getUid(command);

    CreateDirectoryEntry createDirectoryEntry = Transformer.getCreateDirectoryEntry(command);

    for (UserCertificate userCertificate : createDirectoryEntry.getUserCertificates()) {
      try {
        String cn = userCertificate.getDn().getCn();
        response = deleteSingleCertificate(uid, cn, apiClient);
        if (response.getStatusCode() == HttpStatus.SC_OK) {
          sb.append(
              "\nCertificate successful deleted: \n" + response.getData() + " Responce status was: "
                  + response.getStatusCode());
        }
      } catch (ApiException ex) {
        runSucsessfull = false;
        errorCode = ex.getCode();
        sb.append("\nSomething went wrong while deleting certificate. Response status was: "
            + ex.getCode()
            + " certificate: " + userCertificate.getUserCertificate());
      }
    }
    if (!runSucsessfull) {
      throw new ApiException(errorCode, sb + "\n" +
          "At least one certificate could not be deleted in:" + "\n" + Transformer
          .getCreateDirectoryEntry(command));
    }
    return new ExecutionResult(sb.toString(), true, response.getStatusCode());
  }

  private ApiResponse<Void> deleteSingleCertificate(String uid, String certificateEntryId,
      GemApiClient apiClient)
      throws ApiException {
    return new GemCertificateAdministrationApi(apiClient)
        .deleteDirectoryEntryCertificateWithHttpInfo(uid, certificateEntryId);
  }

  @Override
  public boolean postCheck() {
    try {
      super.postCheck();
      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return false;
  }
}
