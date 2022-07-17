package de.gematik.ti.epa.vzd.gem.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti.epa.vzd.gem.CommandNamesEnum;
import de.gematik.ti.epa.vzd.gem.invoker.AccessHandler;
import de.gematik.ti.epa.vzd.gem.invoker.ConfigHandler;
import generated.CommandType;
import generated.UserCertificateType;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class LogHelper {

  final static Logger LOG = getLogger(LogHelper.class);
  final static String LOG_SUM_DIR =
      ConfigHandler.getInstance().getLogSummaryDirectory() + "/vzd-command-log.csv";
  final static String CSV_HEADER = "Date;Time;SystemUser;CredentialsUser;TelematikID;ExecutionCommand;ExecutionStatus;Data";
  public static final String NOT_EXIST_BUT_WAS_CREATED_AT = "Central log summary did not exist, but was created at {}";
  public static final String EXECUTION_TO_CENTRAL_LOG_SUMMARY_AT = "Successfully wrote command execution to central log summary at {}";

  public static void logCommand(CommandNamesEnum execCommand, CommandType command,
      boolean executionStatusOK) {

    StringBuilder logLine = new StringBuilder().append("\n");
    String telematikID = getTelematikID(command);
    String data = objectToString(command);

    logLine.append(LocalDate.now()).append(";");
    logLine.append(LocalTime.now()).append(";");
    logLine.append(System.getProperty("user.name")).append(";");
    logLine.append(AccessHandler.getInstance().getBaseAuth().getUsername()).append(";");
    logLine.append(telematikID).append(";");
    logLine.append(execCommand.toString()).append(";");
    logLine.append(executionStatusOK ? "OK" : "NOK").append(";");
    logLine.append(data);

    try {
      writeToFile(getLogFile(), logLine.toString(), true);
      LOG.debug(EXECUTION_TO_CENTRAL_LOG_SUMMARY_AT, LOG_SUM_DIR);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static File getLogFile() throws IOException {
    File logFile = new File(LOG_SUM_DIR);
    if (logFile.exists()) {
      return logFile;
    }
    return createLogFile(logFile);
  }

  private static File createLogFile(File logFile) throws IOException {
    if (!logFile.getParentFile().exists()) {
      Files.createDirectories(Path.of(logFile.getParent()));
    }
    writeToFile(logFile, CSV_HEADER, false);
    LOG.debug(NOT_EXIST_BUT_WAS_CREATED_AT, logFile.getAbsolutePath());
    return logFile;
  }

  private static void writeToFile(File logFile, String content, boolean append) throws IOException {
    FileOutputStream fileOutputStream = new FileOutputStream(logFile, append);
    fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    fileOutputStream.flush();
    fileOutputStream.close();
  }

  private static String getTelematikID(CommandType command) {
    ArrayList<String> telematikIDs = new ArrayList<>();
    telematikIDs.add(command.getTelematikID());
    command.getUserCertificate().stream().map(UserCertificateType::getTelematikID)
        .forEach(telematikIDs::add);
    telematikIDs.removeAll(Collections.singleton(null));
    return telematikIDs.isEmpty() ? "no telematikID" :
        (telematikIDs.stream().allMatch(telematikIDs.get(0)::equals)) ? telematikIDs.get(0)
            : "no unique telematikID";
  }

  private static String objectToString(Object specifiedType) {
    ObjectMapper oma = new ObjectMapper();
    String unfilteredString;
    try {
      unfilteredString = oma.writeValueAsString(specifiedType);
    } catch (JsonProcessingException e) {
      return null;
    }
    return filterString(unfilteredString);
  }

  private static String filterString(String unfilteredString) {
    List<String> unfiltered = List.of(unfilteredString.split(","));
    List<String> filtered = new ArrayList<>();
    for (String s : unfiltered) {
      if (!(s.contains("[]") || s.contains("null"))) {
        String s2 = s.replaceAll("[\"\\{\\[\\]\\}]", "")
            .replace("\\n", "")
            .replace(" ", "");
        filtered.add(s2);
      }
    }
    return String.join(",", filtered);
  }
}