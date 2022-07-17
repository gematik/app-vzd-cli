/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.exceptions;

public class GemClientException extends RuntimeException {

  public GemClientException() {
  }

  public GemClientException(String message) {
    super(message);
  }

  public GemClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public GemClientException(Throwable cause) {
    super(cause);
  }

  protected GemClientException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
