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

public class AddDirEntryCertExecutionTest {

    private static IConnectionPool connectionPool = mock(ConnectionPool.class);

    @Test
    public void missingUserCertificateElementTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        assertFalse(addDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void missingUidTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setUserCertificate("SomeUserCertificate");
        command.getUserCertificate().add(userCertificateType);
        assertFalse(addDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void missingUserCertificateTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();

        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        UserCertificateType userCertificateType = new UserCertificateType();

        command.getUserCertificate().add(userCertificateType);
        command.setDn(dn);

        assertFalse(addDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void uidInDnTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();

        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setUserCertificate("SomeUserCertificate");

        command.getUserCertificate().add(userCertificateType);
        command.setDn(dn);

        assertTrue(addDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void uidInCertTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();

        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setUserCertificate("SomeUserCertificate");

        userCertificateType.setDn(dn);
        command.getUserCertificate().add(userCertificateType);

        assertTrue(addDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void uidInCertAndDnTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();

        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        DistinguishedNameType dn2 = new DistinguishedNameType();
        dn2.setUid("SomeUid");
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setUserCertificate("SomeUserCertificate");

        userCertificateType.setDn(dn);
        command.getUserCertificate().add(userCertificateType);
        command.setDn(dn2);

        assertTrue(addDirEntryCertExecution.checkValidation(command));
    }

    @Test
    public void mismatchingUidTest() {
        AddDirEntryCertExecution addDirEntryCertExecution = new AddDirEntryCertExecution(connectionPool);
        CommandType command = new CommandType();

        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        DistinguishedNameType dn2 = new DistinguishedNameType();
        dn2.setUid("SomeOtherUid");
        UserCertificateType userCertificateType = new UserCertificateType();
        userCertificateType.setUserCertificate("SomeUserCertificate");

        userCertificateType.setDn(dn);
        command.getUserCertificate().add(userCertificateType);
        command.setDn(dn2);

        assertFalse(addDirEntryCertExecution.checkValidation(command));
    }


}