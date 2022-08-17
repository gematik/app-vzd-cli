package de.gematik.ti.epa.vzd.gem.exceptions;

public class WrongConnection extends RuntimeException {

  public WrongConnection() {
    super("Connection is not associated with this ConnectionPool");
  }
}
