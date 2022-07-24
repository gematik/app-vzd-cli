package de.gematik.ti.epa.vzd.gem.invoker;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

public interface TokenProvider {
    public boolean validateToken() throws OAuthProblemException, OAuthSystemException;
    public String getAccessToken() throws OAuthProblemException, OAuthSystemException;
}
