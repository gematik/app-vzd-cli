package de.gematik.ti.epa.vzd.gem.command.parser;

import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import generated.CommandType;
import generated.DistinguishedNameType;
import generated.UserCertificateType;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ReadListParser implements CommandParser {

  private static final String name = "readDirectoryEntries";

  @Override
  public List<CommandType> buildCommands() {
    List<CommandType> commands = new ArrayList<>();
    List<String> lines = getLines();
    AtomicReference<Integer> count = new AtomicReference<>(0);
    switch (ConfigHandler.getInstance().getListType()) {
      case "uid":
        parseUids(commands, lines, count);
        break;
      case "telematikId":
        parseTelematikIds(commands, lines, count);
        break;
      default:
        throw new CommandException("No or not known listType named: " + ConfigHandler.getInstance().getListType());
    }
    return commands;
  }

  private void parseTelematikIds(List<CommandType> commands, List<String> lines,
      AtomicReference<Integer> count) {
    lines.forEach(telematiId -> {
      CommandType command = new CommandType();
      command.setName(name);
      count.set(count.get() + 1);
      command.setCommandId(String.valueOf(count.get()));
      UserCertificateType userCert = new UserCertificateType();
      userCert.setTelematikID(telematiId);
      command.getUserCertificate().add(userCert);
      command.setTelematikID(telematiId);
      commands.add(command);
    });
  }

  private void parseUids(List<CommandType> commands, List<String> lines,
      AtomicReference<Integer> count) {
    lines.forEach(id -> {
      CommandType command = new CommandType();
      command.setName(name);
      count.set(count.get() + 1);
      command.setCommandId(String.valueOf(count.get()));
      DistinguishedNameType dn = new DistinguishedNameType();
      dn.setUid(id);
      command.setDn(dn);
      commands.add(command);
    });
  }

  private List<String> getLines() {
    List<String> uids = new ArrayList<>();
    String path = ConfigHandler.getInstance().getCommandsPath();
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      String line = br.readLine();
      while (StringUtils.isNotBlank(line)) {
        uids.add(line);
        line = br.readLine();
      }
    } catch (IOException e) {
      throw new CommandException("The command file you have provided is not valid: " + path);
    }
    return uids;
  }
}
