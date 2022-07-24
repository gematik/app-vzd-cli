package de.gematik.ti.epa.vzd.gem.invoker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.gematik.ti.epa.vzd.client.invoker.auth.HttpBasicAuth;
import de.gematik.ti.epa.vzd.gem.exceptions.GemClientException;
import de.gematik.ti.epa.vzd.oauth2.URLConnectionClient;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.LocalDateTime;

public class AccessHandler implements TokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AccessHandler.class);

  private String accessToken;

  private LocalDateTime tokenvalidationDate;
  private HttpBasicAuth baseAuth;

  private AccessHandler() {
    baseAuth = getHttpBasicAuthFromFile();
  }

  private String credentialPath;

  AccessHandler(String credentialPath) {
    this.credentialPath = credentialPath;
  }
  /*
  public OAuth getOAuth2Token() throws OAuthSystemException, OAuthProblemException {
    if (oAuth2Token == null) {
      oAuth2Token = getNewOAuth2Token();
    }
    validateToken();
    return oAuth2Token;
  }
*/
  /**
   * Requests an OAuth2 Token with Username and Password
   *
   * @return
   * @throws OAuthSystemException
   * @throws OAuthProblemException
   */
  @Override
  public String getAccessToken() throws OAuthProblemException, OAuthSystemException {
    LOG.debug("Trying to get new access token");

    OAuthClientRequest request = OAuthClientRequest
        .tokenLocation(ConfigHandler.getInstance().getRetryingOAuthPath())
        .setClientId(getBaseAuth().getUsername())
        .setClientSecret(getBaseAuth().getPassword())
        .setGrantType(GrantType.CLIENT_CREDENTIALS)
        .buildBodyMessage();

    request.setHeader("Accept", "application/json");

    OAuthClient oAuthClient;

    ConfigHandler configHandler = ConfigHandler.getInstance();
    if (configHandler.isProxySet()) {
      Proxy proxy = new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(configHandler.getProxyHost(), configHandler.getProxyPort()));
      oAuthClient = new OAuthClient(new URLConnectionClient(proxy));
    } else {
      oAuthClient = new OAuthClient(new URLConnectionClient());
    }

    OAuthAccessTokenResponse oAuthResponse = oAuthClient
        .accessToken(request, OAuthJSONAccessTokenResponse.class);

    JsonObject jObj = new JsonParser().parse(oAuthResponse.getBody()).getAsJsonObject();
    String accessToken = jObj.get("access_token").toString().replaceAll("\"", "");
    setTokenValidation(jObj.get("expires_in").toString());
    LOG.debug("Requesting new OAuth2 token successful");
    return accessToken;
  }

  /**
   * Sets the time - 10% until the token expires. This ensures that the token is every time valid
   *
   * @param expires_in is an String with numbers. For example 3600 equals 1 hour.
   */
  private void setTokenValidation(String expires_in) {
    int seconds = Integer.parseInt(expires_in);
    int secureSeconds = (int) (seconds * 0.90);
    tokenvalidationDate = LocalDateTime.now().plusSeconds(secureSeconds);
  }

  /**
   * Checks if the token is still valid. If not request a new one
   *
   * @return
   */
  public boolean validateToken() {
    if (LocalDateTime.now().isBefore(tokenvalidationDate)) {
      return true;
    }
    try {
      getAccessToken();
    } catch (OAuthProblemException | OAuthSystemException e) {
      throw new GemClientException("Requesting a new OAuth2 token failed.", e);
    }
    return LocalDateTime.now().isBefore(tokenvalidationDate);
  }

  private HttpBasicAuth getBaseAuth() {
    if (baseAuth == null) {
      baseAuth = getHttpBasicAuthFromFile();
    }
    return baseAuth;
  }

  /**
   * Reads the credentialFile and stores the client_id and client_secret
   *
   * @return
   */
  private HttpBasicAuth getHttpBasicAuthFromFile() {
    String client_id = "";
    String client_secret = "";
    File file = new File(this.credentialPath);

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line = br.readLine();
      while (line != null) {
        String[] param = line.split("=");
        switch (param[0]) {
          case "id":
            client_id = param[1];
            break;
          case "secret":
            client_secret = param[1];
            break;
          default:
            break;
        }
        line = br.readLine();
      }
      HttpBasicAuth basicAuth = new HttpBasicAuth();
      basicAuth.setPassword(client_secret);
      basicAuth.setUsername(client_id);
      return basicAuth;
    } catch (IOException e) {
      LOG.error(
          "The named file on path " + file.getAbsolutePath() + " could not be accessed");
      throw new IllegalArgumentException(
          "The named file on path " + file.getAbsolutePath() + " could not be accessed");
    }
  }

}
