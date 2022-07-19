package de.gematik.ti.epa.vzd.gem.invoker;

import de.gematik.ti.epa.vzd.gem.exceptions.ConnectionAlreadyReleased;
import de.gematik.ti.epa.vzd.gem.exceptions.WrongConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionPool implements IConnectionPool {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionPool.class);

  private final LinkedBlockingQueue<GemApiClient> connections;
  private final List<GemApiClient> usedConnections;

  private ConnectionPool(int size) {
    connections = new LinkedBlockingQueue<>(size);
    usedConnections = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      connections.add(new GemApiClient(this));
    }
  }

  public static IConnectionPool createConnectionPool(final int size) {
    return new ConnectionPool(size);
  }

  @Override
  public synchronized GemApiClient getConnection() throws InterruptedException {
    LOG.trace(
        "Try to get Connection (" + getUsedConnectionSize() + " / " + getConnectionSize() + ")");
    GemApiClient connection = connections.take();
    usedConnections.add(connection);
    LOG.trace(
        "Connection found and return (" + getUsedConnectionSize() + " / " + getConnectionSize()
            + ")");
    return connection;
  }

  @Override
  public synchronized void releaseConnection(GemApiClient gemApiClient) {
    LOG.trace(
        "Try to releaseConnection (" + getUsedConnectionSize() + " / " + getConnectionSize() + ")");
    if (usedConnections.contains(gemApiClient)) {
      usedConnections.remove(gemApiClient);
      connections.add(gemApiClient);
      LOG.trace(
          "Connection released (" + getUsedConnectionSize() + " / " + getConnectionSize() + ")");
    } else if (!connections.contains(gemApiClient)) {
      throw new WrongConnection();
    } else {
      throw new ConnectionAlreadyReleased();
    }
  }

  @Override
  public void reset() {
    connections.addAll(usedConnections);
    usedConnections.clear();
    LOG.trace(
        "Connection reset done (" + getUsedConnectionSize() + " / " + getConnectionSize() + ")");
  }

  @Override
  public int getConnectionSize() {
    return connections.size() + connections.remainingCapacity();
  }

  @Override
  public int getAvailableConnectionSize() {
    return connections.size();
  }

  @Override
  public int getUsedConnectionSize() {
    return usedConnections.size();
  }
}
