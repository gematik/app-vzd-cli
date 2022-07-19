package de.gematik.ti.epa.vzd.gem.testSuites;

import de.gematik.ti.epa.vzd.gem.command.CommandsBuilderTest;
import de.gematik.ti.epa.vzd.gem.command.ExecutionCollectionTest;
import de.gematik.ti.epa.vzd.gem.command.TransformerTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.AddDirEntryCertExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.AddDirEntryExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.DeleteDirEntryCertExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.DeleteDirEntryExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ExecutionBaseTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ModifyDirEntryExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ReadDirEntryCertExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.ReadDirEntryExecutionTest;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.SafeModifyDirExecutionTest;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandlerTest;
import de.gematik.ti.epa.vzd.gem.utils.LogHelperTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    // CommandExecutions
    AddDirEntryCertExecutionTest.class,
    AddDirEntryExecutionTest.class,
    DeleteDirEntryExecutionTest.class,
    DeleteDirEntryCertExecutionTest.class,
    ModifyDirEntryExecutionTest.class,
    ReadDirEntryCertExecutionTest.class,
    ReadDirEntryExecutionTest.class,
    ExecutionBaseTest.class,
    SafeModifyDirExecutionTest.class,
    // Command
    CommandsBuilderTest.class,
    ExecutionCollectionTest.class,
    TransformerTest.class,
    // Invoker
    ConfigHandlerTest.class,
    //Utils
    LogHelperTest.class
})
public class UnitTestsuite {

}
