/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.exceptions;

/**
 * Exception that should be thrown if something went wrong during executing commands
 */
public class CommandException extends RuntimeException {

  public CommandException() {
  }

  public CommandException(String message) {
    super(message);
  }

  public CommandException(String message, Throwable cause) {
    super(message, cause);
  }

  public CommandException(Throwable cause) {
    super(cause);
  }

  protected CommandException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
