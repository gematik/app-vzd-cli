package de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto;

public class ExecutionResult {

  private String message;
  private Boolean result;
  private int httpStatusCode;

  public ExecutionResult(String message, Boolean result) {
    this.message = message;
    this.result = result;
  }

  public ExecutionResult(String message, Boolean result, int httpStatusCode) {
    this.message = message;
    this.result = result;
    this.httpStatusCode = httpStatusCode;
  }

  public String getMessage() {
    return message;
  }

  public Boolean getResult() {
    return result;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setResult(Boolean result) {
    this.result = result;
  }

  public void setHttpStatusCode(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
  }

}
