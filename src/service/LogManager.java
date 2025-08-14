package service;

import config.AppConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private static final File LOG_FILE = new File("debug.log");

    // Synchronized Methode zum Schreiben in das Log
    public synchronized static void logMessage(int port, String direction, String message) {
        if (!AppConfig.logDebug())
            return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = getCurrentTimestamp();
            String logMessage = String.format("[%s] %d %s %s", timestamp, port, direction, message);
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben in die Log-Datei: " + e);
        }
    }

    // Hilfsmethode für das Erstellen des Zeitstempels
    public static String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
}
