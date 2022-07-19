/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.api;

import de.gematik.ti.epa.vzd.client.api.DirectoryEntryAdministrationApi;
import de.gematik.ti.epa.vzd.client.invoker.ApiCallback;
import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.Pair;
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth;
import de.gematik.ti.epa.vzd.client.model.BaseDirectoryEntry;
import de.gematik.ti.epa.vzd.client.model.CreateDirectoryEntry;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import okhttp3.Call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Overrides all functions of DirectoryEntryAdministration api that build calls for the different
 * commands to add the OAuth2 Token to the header
 */
public class GemDirectoryEntryAdministrationApi extends DirectoryEntryAdministrationApi {

  private GemApiClient localVarApiClient;

  public GemDirectoryEntryAdministrationApi(GemApiClient apiClient) {
    this.localVarApiClient = apiClient;
  }

  @Override
  public Call addDirectoryEntryCall(CreateDirectoryEntry createDirectoryEntry,
      ApiCallback _callback) throws ApiException {
    Object localVarPostBody = createDirectoryEntry;

    // create path and map variables
    String localVarPath = "/DirectoryEntries";

    List<Pair> localVarQueryParams = new ArrayList<>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<>();
    Map<String, String> localVarHeaderParams = new HashMap<>();
    Map<String, String> localVarCookieParams = new HashMap<>();
    Map<String, Object> localVarFormParams = new HashMap<>();
    final String[] localVarAccepts = {
        "application/json"
    };
    final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    final String[] localVarContentTypes = {
        "application/json"
    };
    final String localVarContentType = localVarApiClient
        .selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);
    // add OAuth2 token for authorization
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
            localVarAuthNames, _callback);
  }

  @Override
  public Call deleteDirectoryEntryCall(String uid, ApiCallback _callback)
      throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/DirectoryEntries/{uid}"
        .replaceAll("\\{" + "uid" + "\\}", localVarApiClient.escapeString(uid.toString()));

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();
    final String[] localVarAccepts = {
        "application/json;charset=UTF-8"
    };
    final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }
    // add OAuth2 token for authorization
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = localVarApiClient
        .selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams,
            localVarAuthNames, _callback);
  }

  @Override
  public Call modifyDirectoryEntryCall(String uid, BaseDirectoryEntry baseDirectoryEntry,
      ApiCallback _callback) throws ApiException {
    Object localVarPostBody = baseDirectoryEntry;

    // create path and map variables
    String localVarPath = "/DirectoryEntries/{uid}/baseDirectoryEntries"
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
    // add OAuth2 token for authorization
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    final String[] localVarContentTypes = {
        "application/json"
    };
    final String localVarContentType = localVarApiClient.selectHeaderContentType(
        localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams,
            localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
  }

  @Override
  // tag::readEntryMethod[]
  public Call readDirectoryEntryCall(String uid, String givenName, String sn, String cn,
      String displayName, String streetAddress,
      String postalCode, String countryCode, String localityName, String stateOrProvinceName,
      String title, String organization, String otherName,
      String telematikID, String telematikIDSubStr, String specialization, String domainID,
      String holder, String personalEntry,
      String dataFromAuthority, String professionOID, String entryType, Boolean baseEntryOnly,
      final ApiCallback _callback)
    // end::readEntryMethod[]
      throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/DirectoryEntries";

    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
    if (uid != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("uid", uid));
    }

    if (givenName != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("givenName", givenName));
    }

    if (sn != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("sn", sn));
    }

    if (cn != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("cn", cn));
    }

    if (displayName != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("displayName", displayName));
    }

    if (streetAddress != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("streetAddress", streetAddress));
    }

    if (postalCode != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("postalCode", postalCode));
    }

    if (countryCode != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("countryCode", countryCode));
    }

    if (localityName != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("localityName", localityName));
    }

    if (stateOrProvinceName != null) {
      localVarQueryParams.addAll(
          localVarApiClient.parameterToPair("stateOrProvinceName", stateOrProvinceName));
    }

    if (title != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("title", title));
    }

    if (organization != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("organization", organization));
    }

    if (otherName != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("otherName", otherName));
    }

    if (telematikID != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("telematikID", telematikID));
    }

    if (telematikIDSubStr != null) {
      localVarQueryParams.addAll(
          localVarApiClient.parameterToPair("telematikID-SubStr", telematikIDSubStr));
    }

    if (specialization != null) {
      localVarQueryParams.addAll(
          localVarApiClient.parameterToPair("specialization", specialization));
    }

    if (domainID != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("domainID", domainID));
    }

    if (holder != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("owner", holder));
    }

    if (personalEntry != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("personalEntry", personalEntry));
    }

    if (dataFromAuthority != null) {
      localVarQueryParams.addAll(
          localVarApiClient.parameterToPair("dataFromAuthority", dataFromAuthority));
    }

    if (baseEntryOnly != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("baseEntryOnly", baseEntryOnly));
    }

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

    // add OAuth2 token for authorization
    final OAuth oAuth2Token = (OAuth) localVarApiClient.getAuthentication("OAuth");
    localVarHeaderParams.put("Authorization", "Bearer " + oAuth2Token.getAccessToken());

    final String[] localVarContentTypes = {

    };
    final String localVarContentType = localVarApiClient.selectHeaderContentType(
        localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);

    String[] localVarAuthNames = new String[]{"OAuth2"};
    return localVarApiClient
        .buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams,
            localVarPostBody, localVarHeaderParams,
            localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
  }
}
