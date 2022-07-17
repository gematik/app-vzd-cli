package de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto;

public class BaseExecutionResult {

  private final String name;
  private final Boolean result;

  public BaseExecutionResult(String name, Boolean result) {
    this.name = name;
    this.result = result;
  }

  public String getName() {
    return name;
  }

  public Boolean getResult() {
    return result;
  }
}
