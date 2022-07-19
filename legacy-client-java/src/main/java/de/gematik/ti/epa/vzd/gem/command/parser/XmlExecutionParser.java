package de.gematik.ti.epa.vzd.gem.command.parser;

import de.gematik.ti.epa.vzd.gem.command.CommandsBuilder;
import de.gematik.ti.epa.vzd.gem.exceptions.CommandException;
import de.gematik.ti.epa.vzd.gem.exceptions.ReadException;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import generated.CommandListType;
import generated.CommandType;
import generated.ObjectFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jakarta.xml.bind.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XmlExecutionParser implements CommandParser {

  private JAXBContext jaxbContext;
  private Logger LOG = LoggerFactory.getLogger(CommandsBuilder.class);

  private static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

  public XmlExecutionParser() {
    try {
      jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
    } catch (JAXBException e) {
      throw new ReadException("Error occurred by creating JAXBContext");
    }
  }


  public List<CommandType> buildCommands() {
    ConfigHandler configHandler = ConfigHandler.getInstance();
    CommandListType commandList;
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      Document doc = builderFactory.newDocumentBuilder()
          .parse(new File(configHandler.getCommandsPath()));
      Object obj = unmarshaller.unmarshal(doc);
      Object commands = ((JAXBElement) obj).getValue();
      if (commands instanceof CommandListType) {
        commandList = (CommandListType) commands;
        if (addIds(commandList)) {
          writeCommandDataWithIds(commandList);
        }
        LOG.debug("Commands have been build");
        return format(commandList.getCommand());
      }
    } catch (ParserConfigurationException | SAXException | JAXBException e) {
      throw new ReadException(
          "An error have been occurred while reading your command file. Please check if this file is a valid .xml file");
    } catch (IOException e) {
      throw new ReadException(
          "A problem with your named file have occurred. Please if check "
              + configHandler.getCommandsPath() + " exist");
    }
    return null;
  }

  private List<CommandType> format(List<CommandType> command) {
    command.forEach(cert -> cert.getUserCertificate().forEach(c -> {
      if (StringUtils.isNotBlank(c.getUserCertificate())) {
        c.setUserCertificate(c.getUserCertificate().replaceAll("\\s", ""));
      }
    }));
    return command;
  }

  private boolean addIds(CommandListType commandList) {
    int counter = 1;
    boolean idsSet = false;
    Set<String> givenIds = getAllGivenIds(commandList);
    for (CommandType command : commandList.getCommand()) {
      counter = getNewUniqueCounter(counter, givenIds);
      if (StringUtils.isBlank(command.getCommandId())) {
        command.setCommandId(String.valueOf(counter));
        idsSet = true;
      }
    }
    if (idsSet) {
      LOG.debug("IDs have been set automatically");
    }
    return idsSet;
  }

  private int getNewUniqueCounter(int counter, Set<String> givenIds) {
    while (givenIds.contains(String.valueOf(counter))) {
      counter++;
    }
    givenIds.add(String.valueOf(counter));
    return counter;
  }

  private Set<String> getAllGivenIds(CommandListType commandList) {
    Set<String> givenIds = new HashSet<>();
    for (CommandType command : commandList.getCommand()) {
      if (!StringUtils.isBlank(command.getCommandId()) && !givenIds.add(command.getCommandId())) {
        LOG.error("The predefined ID \"" + command.getCommandId() + "\" occurs twice");
        throw new CommandException(
            "The predefined ID \"" + command.getCommandId() + "\" occurs twice");
      }
    }
    return givenIds;
  }

  private void writeCommandDataWithIds(CommandListType commandList) throws JAXBException {
    JAXBElement<CommandListType> element = new ObjectFactory().createCommandList(commandList);
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.marshal(element, new File(ConfigHandler.getInstance().getCommandsPath()));
  }
}
