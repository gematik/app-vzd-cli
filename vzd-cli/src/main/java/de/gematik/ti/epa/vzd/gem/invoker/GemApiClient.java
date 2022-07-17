/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.invoker;

import de.gematik.ti.epa.vzd.client.invoker.ApiClient;
import de.gematik.ti.epa.vzd.client.invoker.JSON;
import de.gematik.ti.epa.vzd.client.invoker.auth.Authentication;
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuthFlow;
import de.gematik.ti.epa.vzd.client.invoker.auth.RetryingOAuth;
import de.gematik.ti.epa.vzd.gem.exceptions.GemClientException;
import okhttp3.OkHttpClient;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GemApiClient extends ApiClient implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(GemApiClient.class);

  private String retryingOAuthPath = ConfigHandler.getInstance().getRetryingOAuthPath();
  private Map<String, Authentication> authentications;
  private IConnectionPool connectionPool;
  private Proxy proxy;


  public GemApiClient(IConnectionPool connectionPool) {

    ConfigHandler configHandler = ConfigHandler.getInstance();
    if (configHandler.isProxySet()) {
      this.proxy = new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(configHandler.getProxyHost(), configHandler.getProxyPort()));
    } else {
      this.proxy = Proxy.NO_PROXY;
    }

    this.connectionPool = connectionPool;
    modifiedWithOAuth();
    this.authentications = Collections.unmodifiableMap(authentications);
  }

  /*
   * Constructor for ApiClient to support access token retry on 401/403 configured with client ID
   */
  public GemApiClient(final String clientId) {
    this(clientId, null, null);
  }

  /*
   * Constructor for ApiClient to support access token retry on 401/403 configured with client ID and additional parameters
   */
  public GemApiClient(final String clientId, final Map<String, String> parameters) {
    this(clientId, null, parameters);
  }

  /*
   * Constructor for ApiClient to support access token retry on 401/403 configured with client ID, secret, and additional parameters
   */
  public GemApiClient(final String clientId, final String clientSecret,
      final Map<String, String> parameters) {
    modifiedWithOAuth();

    final RetryingOAuth retryingOAuth = new RetryingOAuth(retryingOAuthPath, clientId,
        OAuthFlow.application, clientSecret, parameters);
    authentications.put("OAuth2", retryingOAuth);
    getHttpClient().interceptors().add(retryingOAuth);

    // Prevent the authentications from being modified.
    authentications = Collections.unmodifiableMap(authentications);
  }

  /**
   * The function <code> getProgressInterceptor </code> comes from the generated class ApiClient and
   * maybe have to be set on public again.
   * <p>
   * The OAuth2 token is stored in authentications.get("OAuth")
   */
  private void modifiedWithOAuth() {
    ConfigHandler configHandler = ConfigHandler.getInstance();
    setBasePath(configHandler.getBasePath());

    OkHttpClient.Builder builder;
    builder = new OkHttpClient.Builder().proxy(proxy)
        .readTimeout(configHandler.getTimeout(), TimeUnit.SECONDS);

    // Function have to be set public when client is regenerated
    builder.addNetworkInterceptor(getProgressInterceptor());
    setHttpClient(builder.build());

    setVerifyingSsl(true);

    setJSON(new JSON());

    // Set default User-Agent.
    setUserAgent("OpenAPI-Generator/1.0.0/java");

    authentications = new HashMap<>();
    authentications
        .put("HttpBasicAuth", AccessHandler.getInstance().getBaseAuth());
    try {
      authentications.put("OAuth", AccessHandler.getInstance().getOAuth2Token());
    } catch (OAuthSystemException | OAuthProblemException e) {
      LOG.error("Error while getting Token");
      throw new ExceptionInInitializerError("Error while getting Token");
    }
  }

  /**
   * Checks if token is still valid and if not requests a new one
   */
  public void validateToken() {
    try {
      AccessHandler.getInstance().getOAuth2Token();
    } catch (OAuthSystemException | OAuthProblemException e) {
      LOG.error("Error while refreshing OAuthToken");
      throw new GemClientException("Error while refreshing OAuthToken");
    }
  }

  // <editor-fold desc="Getter & Setter">

  /**
   * Get authentications (key: authentication name, value: authentication).
   *
   * @return Map of authentication objects
   */
  @Override
  public Map<String, Authentication> getAuthentications() {
    return authentications;
  }

  /**
   * Get authentication for the given name.
   *
   * @param authName The authentication name
   * @return The authentication, null if not found
   */
  @Override
  public Authentication getAuthentication(final String authName) {
    return authentications.get(authName);
  }

  @Override
  public void close() throws Exception {
    if (connectionPool != null) {
      connectionPool.releaseConnection(this);
    }
  }

  // </editor-fold>
}
