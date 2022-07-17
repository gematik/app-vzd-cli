/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.BaseDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.CreateDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.DistinguishedName;
import de.gematik.ti.epa.vzd.client.model.UserCertificate;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemCertificateAdministrationApi;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.DistinguishedNameType;
import generated.UserCertificateType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific execution for Command "AddDirectoryEntryCertificate"
 */
public class AddDirEntryCertExecution extends ExecutionBase {

  private Logger LOG = LoggerFactory.getLogger(AddDirEntryCertExecution.class);

  public AddDirEntryCertExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.ADD_DIR_CERT);
  }

  @Override
  public boolean checkValidation(CommandType command) {
    if (command.getUserCertificate().isEmpty()) {
      LOG.error("No certificate element found");
      return false;
    }
    String uid = null;
    if (command.getDn() != null) {
      uid = command.getDn().getUid();
    }
    for (UserCertificateType cert : command.getUserCertificate()) {
      if (StringUtils.isBlank(cert.getUserCertificate())) {
        LOG.error("No user certificate for element found");
        return false;
      }
      if (cert.getDn() != null) {
        DistinguishedNameType certDn = cert.getDn();
        if (uid == null) {
          uid = certDn.getUid();
        }
        if (StringUtils.isNotBlank(certDn.getUid())) {
          if (!uid.equals(certDn.getUid())) {
            LOG.error("Mismatching uid delivered");
            return false;
          }
        }
      }
    }
    if (StringUtils.isBlank(uid)) {
      LOG.error("No or mismatching uid delivered");
      return false;
    }
    return true;
  }

  @Override
  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException {
    StringBuffer sb = new StringBuffer();
    ApiResponse<DistinguishedName> response = null;
    boolean runSuccessful = true;
    int errorCode = 0;

    CreateDirectoryEntry createDirectoryEntry = Transformer.getCreateDirectoryEntry(command);

    for (UserCertificate userCertificate : createDirectoryEntry.getUserCertificates()) {
      try {
        String uid = getUid(createDirectoryEntry.getDirectoryEntryBase(), userCertificate);
        response = addSingleCertificate(uid, userCertificate, apiClient);
        if (response.getStatusCode() == HttpStatus.SC_CREATED) {
          sb.append(
              "\nCertificate successful added: \n" + response.getData() + " Responce status was: "
                  + response.getStatusCode());
        }
      } catch (ApiException ex) {
        runSuccessful = false;
        errorCode = ex.getCode();
        sb.append(
            "\nSomething went wrong while adding certificate. Response status was: " + ex.getCode()
                + " certificate: " + userCertificate.getUserCertificate());
      }
    }
    if (!runSuccessful) {
      throw new ApiException(errorCode, sb + "\n" +
          "At least one certificate could not be added in:" + "\n" + Transformer
          .getCreateDirectoryEntry(command));
    }
    return new ExecutionResult(sb.toString(), true, response.getStatusCode());
  }

  private String getUid(BaseDirectoryEntry directoryEntryBase, UserCertificate userCertificate) {
    String uidCert = null;
    String uidEntry = null;

    if (userCertificate.getDn() != null) {
      uidCert = userCertificate.getDn().getUid();
    }

    if (directoryEntryBase != null) {
      DistinguishedName dn = directoryEntryBase.getDn();
      if (dn != null) {
        uidEntry = dn.getUid();
      }
    }
    return uidCert == null ? uidEntry : uidCert;
  }

  private ApiResponse<DistinguishedName> addSingleCertificate(String uid,
      UserCertificate userCertificate, GemApiClient apiClient)
      throws ApiException {
    return new GemCertificateAdministrationApi(apiClient)
        .addDirectoryEntryCertificateWithHttpInfo(uid, userCertificate);
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
