package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import de.gematik.ti.epa.vzd.gem.invoker.ConnectionPool;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import generated.DistinguishedNameType;
import generated.UserCertificateType;
import org.junit.Test;

public class ReadDirEntryCertExecutionTest {

    private static IConnectionPool connectionPool = mock(ConnectionPool.class);

    @Test
    public void noCertificatePresentTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        assertFalse(readDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void emptyCertificateTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        command.getUserCertificate().add(userCertificateType);
        assertFalse(readDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void uidPresentTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        userCertificateType.setDn(dn);
        command.getUserCertificate().add(userCertificateType);
        assertTrue(readDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void entryTypePresentTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setEntryType("1");
        command.getUserCertificate().add(userCertificateType);
        assertTrue(readDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void telematikIDPresentTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setTelematikID("SomeTelematikId");
        command.getUserCertificate().add(userCertificateType);
        assertTrue(readDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void professionOIDPresentTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setProfessionOID("SomeProfessionOID");
        command.getUserCertificate().add(userCertificateType);
        assertTrue(readDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void usagePresentTest() {
        ReadDirEntryCertExecution readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.getUsage().add("SomeUsage");
        command.getUserCertificate().add(userCertificateType);
        assertTrue(readDirEntryCertExecution.checkValidation(command));
    }

}