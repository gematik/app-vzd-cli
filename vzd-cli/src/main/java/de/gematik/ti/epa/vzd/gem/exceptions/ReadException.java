/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.exceptions;

/**
 * Exception that should be thrown when something while reading files went wrong
 */
public class ReadException extends RuntimeException {

  public ReadException() {
  }

  public ReadException(String message) {
    super(message);
  }

  public ReadException(String message, Throwable cause) {
    super(message, cause);
  }

  public ReadException(Throwable cause) {
    super(cause);
  }

  protected ReadException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
