package de.gematik.ti.epa.vzd.gem.api;

import de.gematik.ti.epa.vzd.client.api.GetInfoDirectoryEntryAdministrationApi;
import de.gematik.ti.epa.vzd.client.invoker.ApiCallback;
import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.Pair;
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GemGetInfoDirectoryEntryAdministrationApi extends
    GetInfoDirectoryEntryAdministrationApi {

  private GemApiClient localVarApiClient;

  public GemGetInfoDirectoryEntryAdministrationApi(GemApiClient apiClient) {
    this.localVarApiClient = apiClient;
  }

  @Override
  public okhttp3.Call getInfoCall(final ApiCallback _callback) throws ApiException {
    Object localVarPostBody = null;

    // create path and map variables
    String localVarPath = "/";

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
