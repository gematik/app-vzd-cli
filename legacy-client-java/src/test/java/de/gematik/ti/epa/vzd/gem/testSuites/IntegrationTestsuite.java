package de.gematik.ti.epa.vzd.gem.testSuites;

import de.gematik.ti.epa.vzd.gem.command.commandExecutions.AddDirEntryCertExecutionIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.AddDirEntryExecutionIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.DeleteDirEntryExecutionIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ExecutionBaseIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ModifyDirEntryExecutionIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ReadDirEntryCertExecutionIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ReadDirEntryExecutionIntegrationTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ReadDirEntrySyncExecutionIntegrationTest;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@SuiteClasses({
    AddDirEntryCertExecutionIntegrationTest.class,
    AddDirEntryExecutionIntegrationTest.class,
    DeleteDirEntryExecutionIntegrationTest.class,
    ModifyDirEntryExecutionIntegrationTest.class,
    ReadDirEntryCertExecutionIntegrationTest.class,
    ReadDirEntryExecutionIntegrationTest.class,
    ExecutionBaseIntegrationTest.class,
    ReadDirEntrySyncExecutionIntegrationTest.class})
public class IntegrationTestsuite {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestsuite.class);
    public static Process serverProcess;

    @BeforeClass
    public static void startServer() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new StartServer());
        Thread.currentThread().sleep(10000);
        service.shutdown();
    }

    @AfterClass
    public static void tearDownServer() {
        serverProcess.destroy();
    }

    static class StartServer implements Runnable {

        @Override
        public void run() {
            File f = new File("src/test/resources/exec/Testserver.jar");
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                f.getAbsolutePath()
            );
            try {
                System.out.println("startServer");
                serverProcess = pb.start();
                System.out.println("Server started by " + Thread.currentThread().getName());
                BufferedReader input = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
                String line;
                try {
                    while ((line = input.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException ioException) {
                LOG.error("Server could not be started");
            }
        }
    }

}
