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

public class DeleteDirEntryCertExecutionTest {

    private static IConnectionPool connectionPool = mock(ConnectionPool.class);

    @Test
    public void checkValidationMissingUid() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setCn("SomeCn");
        cert.setDn(dn);
        command.getUserCertificate().add(cert);

        assertFalse(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationMissingCnAndUidInCert() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        cert.setDn(dn);
        command.getUserCertificate().add(cert);

        assertFalse(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationMissingCnAndUidInDnFromBaseEntry() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        DistinguishedNameType dn = new DistinguishedNameType();
        DistinguishedNameType dnCert = new DistinguishedNameType();
        dn.setUid("SomeUid");
        command.setDn(dn);
        cert.setDn(dnCert);
        command.getUserCertificate().add(cert);

        assertFalse(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationMultiCertsAndUidInOneCert() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        UserCertificateType cert2 = new UserCertificateType();
        DistinguishedNameType dnCert = new DistinguishedNameType();
        DistinguishedNameType dnCert2 = new DistinguishedNameType();
        dnCert.setCn("SomeCn");
        dnCert2.setCn("SomeOther");
        dnCert2.setUid("SomeUid");
        cert.setDn(dnCert);
        cert2.setDn(dnCert2);
        command.getUserCertificate().add(cert);
        command.getUserCertificate().add(cert2);

        assertTrue(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationMultiCertsAndDifferentUid() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        UserCertificateType cert2 = new UserCertificateType();
        DistinguishedNameType dnCert = new DistinguishedNameType();
        DistinguishedNameType dnCert2 = new DistinguishedNameType();
        dnCert.setCn("SomeCn");
        dnCert.setUid("SomeUid");
        dnCert2.setCn("SomeOther");
        dnCert2.setUid("SomeOther");
        cert.setDn(dnCert);
        cert2.setDn(dnCert2);
        command.getUserCertificate().add(cert);
        command.getUserCertificate().add(cert2);

        assertFalse(deleteDirEntryExecution.checkValidation(command));
    }
    @Test
    public void checkValidationMultiCertsAndDifferentUidToBase() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        UserCertificateType cert2 = new UserCertificateType();
        DistinguishedNameType dnBase = new DistinguishedNameType();
        DistinguishedNameType dnCert = new DistinguishedNameType();
        DistinguishedNameType dnCert2 = new DistinguishedNameType();
        dnCert.setCn("SomeCn");
        dnCert.setUid("SomeUid");
        dnCert2.setCn("SomeOther");
        dnCert2.setUid("SomeUid");
        dnBase.setUid("SomeOtherUid");
        cert.setDn(dnCert);
        cert2.setDn(dnCert2);
        command.setDn(dnBase);
        command.getUserCertificate().add(cert);
        command.getUserCertificate().add(cert2);

        assertFalse(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationMultiCertsAndSameUid() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        UserCertificateType cert2 = new UserCertificateType();
        DistinguishedNameType dnCert = new DistinguishedNameType();
        DistinguishedNameType dnCert2 = new DistinguishedNameType();
        dnCert.setCn("SomeCn");
        dnCert.setUid("SomeUid");
        dnCert2.setCn("SomeOther");
        dnCert2.setUid("SomeUid");
        cert.setDn(dnCert);
        cert2.setDn(dnCert2);
        command.getUserCertificate().add(cert);
        command.getUserCertificate().add(cert2);

        assertTrue(deleteDirEntryExecution.checkValidation(command));
    }

    @Test
    public void checkValidationHaveUIDAndCert() {
        DeleteDirEntryCertExecution deleteDirEntryExecution = new DeleteDirEntryCertExecution(connectionPool);

        CommandType command = new CommandType();
        UserCertificateType cert = new UserCertificateType();
        DistinguishedNameType dn = new DistinguishedNameType();
        dn.setUid("SomeUid");
        dn.setCn("SomeCn");
        cert.setDn(dn);
        command.getUserCertificate().add(cert);

        assertTrue(deleteDirEntryExecution.checkValidation(command));
    }
}