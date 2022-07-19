package de.gematik.ti.epa.vzd.gem.command.commandExecutions;

import de.gematik.ti.epa.vzd.client.invoker.ApiException;
import de.gematik.ti.epa.vzd.client.invoker.ApiResponse;
import de.gematik.ti.epa.vzd.client.model.InfoObject;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.api.GemGetInfoDirectoryEntryAdministrationApi;
import de.gematik.ti.epa.vzd.gem.command.Transformer;
import de.gematik.ti.epa.vzd.gem.command.commandExecutions.dto.ExecutionResult;
import de.gematik.ti.epa.vzd.gem.invoker.GemApiClient;
import de.gematik.ti.epa.vzd.gem.invoker.IConnectionPool;
import generated.CommandType;
import org.apache.http.HttpStatus;

import javax.xml.datatype.DatatypeConfigurationException;

public class GetInfoExecution extends ExecutionBase {

  public GetInfoExecution(IConnectionPool connectionPool) {
    super(connectionPool, CommandNamesEnum.GET_INFO);
  }

  @Override
  public boolean checkValidation(CommandType command) {
    return true;
  }

  @Override
  protected ExecutionResult executeCommand(CommandType command, GemApiClient apiClient)
      throws ApiException, DatatypeConfigurationException {
    ApiResponse<InfoObject> response = new GemGetInfoDirectoryEntryAdministrationApi(
        apiClient).getInfoWithHttpInfo();
    if (response.getStatusCode() == HttpStatus.SC_OK) {
      return new ExecutionResult("\nGet info execution successful operated\n" + response.getData(),
          true, response.getStatusCode());
    }
    throw new ApiException(response.getStatusCode(),
        "Add directory entry execution failed. Response status was: "
            + response.getStatusCode() + "\n"
            + Transformer.getCreateDirectoryEntry(command));
  }
}
