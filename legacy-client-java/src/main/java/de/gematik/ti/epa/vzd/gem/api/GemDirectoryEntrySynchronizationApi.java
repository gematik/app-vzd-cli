package de.gematik.ti.epa.vzd.gem.api;

import de.gematik.ti.epa.vzd.client.api.DirectoryEntrySynchronizationApi;
import de.gematik.ti.epa.vzd.client.invoker.ApiCallback;
import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.Pair;
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GemDirectoryEntrySynchronizationApi extends DirectoryEntrySynchronizationApi {

  private GemApiClient localVarApiClient;

  public GemDirectoryEntrySynchronizationApi(GemApiClient apiClient) {
    this.localVarApiClient = apiClient;
  }

  @Override
  public okhttp3.Call readDirectoryEntryForSyncCall(String uid, String givenName, String sn,
      String cn, String displayName, String streetAddress,
      String postalCode, String countryCode, String localityName, String stateOrProvinceName,
      String title, String organization, String otherName,
      String telematikID, String telematikIDSubStr, String specialization, String domainID,
      String owner, String personalEntry,
      String dataFromAuthority, Boolean baseEntryOnly, final ApiCallback _callback)
      throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/DirectoryEntriesSync";

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

    if (owner != null) {
      localVarQueryParams.addAll(localVarApiClient.parameterToPair("owner", owner));
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
