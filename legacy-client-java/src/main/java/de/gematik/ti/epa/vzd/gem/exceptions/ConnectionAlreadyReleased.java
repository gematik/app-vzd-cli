package de.gematik.ti.epa.vzd.gem.exceptions;

public class ConnectionAlreadyReleased extends RuntimeException {

  public ConnectionAlreadyReleased() {
    super("The Connection is already released!");
  }
}
