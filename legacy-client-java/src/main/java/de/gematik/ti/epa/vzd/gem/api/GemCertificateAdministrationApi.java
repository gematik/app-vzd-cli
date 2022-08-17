/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.api;

import de.gematik.ti.epa.vzd.client.api.CertificateAdministrationApi;
import de.gematik.ti.epa.vzd.client.invoker.ApiCallback;
import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.Pair;
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth;
import de.gematik.ti.epa.vzd.client.model.UserCertificate;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Overrides all functions of CertificateAdministration api that build calls for the different
 * commands to add the OAuth2 Token to the header
 */
public class GemCertificateAdministrationApi extends CertificateAdministrationApi {

  private GemApiClient localVarApiClient;

  public GemCertificateAdministrationApi(GemApiClient apiClient) {
    this.localVarApiClient = apiClient;
  }

  @Override
  // tag::readCertMethod[]
  public okhttp3.Call readDirectoryCertificatesCall(String uid, String certificateEntryID,
      String entryType, String telematikID, String professionOID, String usage,
      final ApiCallback _callback)
    // end::readCertMethod[]
      throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/DirectoryEntries/Certificates";

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    if (uid != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("uid", uid));
    }

    if (certificateEntryID != null) {
      localVarQueryParams.addAll(
          localVarApiClient.parameterToPair("certificateEntryID", certificateEntryID));
    }

    if (entryType != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("entryType", entryType));
    }

    if (telematikID != null) {
      localVarQueryParams
          .addAll(localVarApiClient.parameterToPair("telematikID", telematikID));
    }

    if (professionOID != null) {
      localVarQueryParams
          .addAll(localVarApiClient.parameterToPair("professionOID", professionOID));
    }

    if (usage != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("usage", usage));
    }

    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();
    final String[] localVarAccepts = {"application/json"};

    final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }
    //Setze Auth token
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = localVarApiClient
        .selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
            localVarAuthNames, _callback);
  }

  @Override
  public okhttp3.Call addDirectoryEntryCertificateCall(String uid,
      UserCertificate userCertificate, final ApiCallback _callback) throws ApiException {
    Object localVarPostBody = userCertificate;

    // create path and map variables
    String localVarPath = "/DirectoryEntries/{uid}/Certificates"
        .replaceAll("\\{" + "uid" + "\\}", localVarApiClient.escapeString(uid.toString()));

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();
    final String[] localVarAccepts = {
        "application/json"
    };
    final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    //Setze Auth token
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    final String[] localVarContentTypes = {
        "application/json"
    };
    final String localVarContentType = localVarApiClient
        .selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
            localVarAuthNames, _callback);
  }

  @Override
  public okhttp3.Call deleteDirectoryEntryCertificateCall(String uid, String certificateEntryID,
      final ApiCallback _callback) throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/DirectoryEntries/{uid}/Certificates/{certificateEntryID}"
        .replaceAll("\\{" + "uid" + "\\}", localVarApiClient.escapeString(uid.toString()))
        .replaceAll("\\{" + "certificateEntryID" + "\\}",
            localVarApiClient.escapeString(certificateEntryID.toString()));

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();

    //Setze Auth token
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    final String[] localVarAccepts = {
        "application/json"
    };
    final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = localVarApiClient.selectHeaderContentType(
        localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams,
            localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
  }

}
