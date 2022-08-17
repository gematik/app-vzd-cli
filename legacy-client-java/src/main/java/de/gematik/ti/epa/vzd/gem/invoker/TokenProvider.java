package de.gematik.ti.epa.vzd.gem.invoker;

import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

public interface TokenProvider {
    public OAuth getOAuth2Token() throws OAuthSystemException, OAuthProblemException;
}
