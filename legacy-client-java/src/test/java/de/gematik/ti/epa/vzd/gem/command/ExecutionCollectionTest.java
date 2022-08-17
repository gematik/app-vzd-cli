package de.gematik.ti.epa.vzd.gem.command;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import org.junit.Test;

public class ExecutionCollectionTest {

    private static ConnectionPool connectionPool = mock(ConnectionPool.class);

    @Test
    public void getInstanceWithoutInit() {
        InstantiationError ex = assertThrows(InstantiationError.class, () -> ExecutionCollection.getInstance());
        assertEquals("Please instance a executor first. It needs an ConnectionPool", ex.getMessage());
    }

    @Test
    public void initWithoutConnectionPool() {
        ExecutionCollection.init(connectionPool);
        InstantiationError ex = assertThrows(InstantiationError.class, () -> ExecutionCollection.init(connectionPool));

        assertEquals("Executor is already instanced", ex.getMessage());
    }
}