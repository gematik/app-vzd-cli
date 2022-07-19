package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.auth.OAuth;
import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.DistinguishedNameType;
import generated.UserCertificateType;
import org.junit.Test;

public class ExecutionBaseTest {

    private static IConnectionPool connectionPool = mock(ConnectionPool.class);
    private static GemApiClient apiClient = mock(GemApiClient.class);
    private static OAuth token = mock(OAuth.class);

    @Test
    public void getUidTestUidInDn() throws ApiException {

        ExecutionBase readDirEntryExecution = new ReadDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        command.setDn(dn);

        String result = readDirEntryExecution.getUid(command, apiClient);

        assertEquals("SomeUid", result);
    }

    @Test
    public void getUidTestUidCert() throws ApiException {

        ExecutionBase readDirEntryExecution = new ReadDirEntryExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        cert.setDn(dn);
        command.getUserCertificate().add(cert);

        String result = readDirEntryExecution.getUid(command, apiClient);

        assertEquals("SomeUid", result);
    }

    @Test
    public void getUidTestNoUid() throws ApiException {

        ExecutionBase readDirEntryExecution = new ReadDirEntryExecution(connectionPool);
        CommandType command = new CommandType();

        String result = readDirEntryExecution.getUid(command, apiClient);

        assertEquals(null, result);
    }

}