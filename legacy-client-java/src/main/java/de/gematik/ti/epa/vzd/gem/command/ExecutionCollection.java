/*
 * ${GEMATIK_COPYRIGHT_STATEMENT}
 */

package de.gematik.ti.epa.vzd.gem.command;

import de.gematik.ti.epa.vzd.gem.command.commandExecutions.*;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that maintains all the specific Executions, have to be initialisized once with a
 * GemApiClient
 */
public class ExecutionCollection {

  private static ExecutionCollection executionCollection;

  private final ReadDirEntryExecution readDirEntryExecution;
  private final ReadDirEntryCertExecution readDirEntryCertExecution;
  private final AddDirEntryExecution addDirEntryExecution;
  private final ModifyDirEntryExecution modifyDirEntryExecution;

  private static Logger LOG = LoggerFactory.getLogger(ExecutionCollection.class);

  private ArrayList<ExecutionBase> executors = new ArrayList<>();


  private ExecutionCollection(IConnectionPool connectionPool) {
    this.readDirEntryExecution = new ReadDirEntryExecution(connectionPool);
    this.readDirEntryCertExecution = new ReadDirEntryCertExecution(connectionPool);
    this.addDirEntryExecution = new AddDirEntryExecution(connectionPool);
    this.modifyDirEntryExecution = new ModifyDirEntryExecution(connectionPool);

    executors.add(readDirEntryExecution);
    executors.add(new ReadDirEntrySyncExecution(connectionPool));
    executors.add(readDirEntryCertExecution);
    executors.add(addDirEntryExecution);
    executors.add(new AddDirEntryCertExecution(connectionPool));
    executors.add(modifyDirEntryExecution);
    executors.add(new SafeModifyDirEntryExecution(connectionPool));
    executors.add(new DeleteDirEntryExecution(connectionPool));
    executors.add(new DeleteDirEntryCertExecution(connectionPool));
    executors.add(new GetInfoExecution(connectionPool));
  }

  /**
   * Gives the instance as long as it exists.
   *
   * @return
   */
  public static ExecutionCollection getInstance() {
    if (executionCollection == null) {
      throw new InstantiationError("Please instance a executor first. It needs an ConnectionPool");
    }
    return executionCollection;
  }

  /**
   * Instances the executions
   *
   * @param connectionPool
   * @return
   */
  public static ExecutionCollection init(IConnectionPool connectionPool) {
    if (executionCollection != null) {
      LOG.error("Error occurred while initializing executions. Executor is already instanced");
      throw new InstantiationError("Executor is already instanced");
    }
    executionCollection = new ExecutionCollection(connectionPool);
    LOG.debug("Executiors have been initialized correctly");

    return executionCollection;
  }

  /**
   * Empties all commands in all executors
   */
  public void resetExecutors() {
    this.executors.forEach(e -> e.reset());
  }

  //<editor-fold desc="Getter">

  public void setExecutionCollection(ExecutionCollection executionCollection) {
    this.executionCollection = executionCollection;
  }

  public List<ExecutionBase> getExecutors() {
    return this.executors;
  }

  public ReadDirEntryExecution getReadDirEntryExecution() {
    return this.readDirEntryExecution;
  }

  public ReadDirEntryCertExecution getReadDirEntryCertExecution() {
    return this.readDirEntryCertExecution;
  }

  public AddDirEntryExecution getAddDirEntryExecution() {
    return this.addDirEntryExecution;
  }

  public ModifyDirEntryExecution getModifyDirEntry() {
    return this.modifyDirEntryExecution;
  }
  //</editor-fold>
}
