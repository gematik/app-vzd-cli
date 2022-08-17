package de.gematik.ti.epa.vzd.gem.command.parser;

import generated.CommandType;

import java.util.List;

public interface CommandParser {

  List<CommandType> buildCommands();
}
