package de.gematik.ti.epa.vzd.gem.invoker;

public interface IConnectionPool {

  GemApiClient getConnection() throws InterruptedException;

  void releaseConnection(GemApiClient gemApiClient);

  void reset();

  int getConnectionSize();

  int getAvailableConnectionSize();

  int getUsedConnectionSize();

}
