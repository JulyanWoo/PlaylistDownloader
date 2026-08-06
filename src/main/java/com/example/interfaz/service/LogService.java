package com.example.interfaz.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LogService {
    private static LogService instance;
    private final Deque<String> logs;
    private final DateTimeFormatter formatter;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private boolean isCapturing = false;

    private LogService() {
        logs = new ArrayDeque<>();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        originalOut = System.out;
        originalErr = System.err;
        startCapturing();
    }

    public static synchronized LogService getInstance() {
        if (instance == null) {
            instance = new LogService();
        }
        return instance;
    }

    public synchronized void startCapturing() {
        if (!isCapturing) {
            isCapturing = true;

            PrintStream customOut = new PrintStream(new OutputStream() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public synchronized void write(int b) throws IOException {
                    originalOut.write(b);

                    char c = (char) b;
                    if (c == '\n') {
                        String line = buffer.toString();
                        if (!line.trim().isEmpty()) {
                            addLog("[INFO] " + line);
                        }
                        buffer.setLength(0);
                    } else {
                        buffer.append(c);
                    }
                }
            }, true);

            PrintStream customErr = new PrintStream(new OutputStream() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public synchronized void write(int b) throws IOException {
                    originalErr.write(b);

                    char c = (char) b;
                    if (c == '\n') {
                        String line = buffer.toString();
                        if (!line.trim().isEmpty()) {
                            addLog("[ERROR] " + line);
                        }
                        buffer.setLength(0);
                    } else {
                        buffer.append(c);
                    }
                }
            }, true);

            System.setOut(customOut);
            System.setErr(customErr);

            addLog("[SYSTEM] LogService iniciado - Captura de logs activada");
        }
    }

    public synchronized void stopCapturing() {
        if (isCapturing) {
            isCapturing = false;
            System.setOut(originalOut);
            System.setErr(originalErr);
            addLog("[SYSTEM] LogService detenido - Captura de logs desactivada");
        }
    }

    public synchronized void addLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] %s", timestamp, message);
        logs.addLast(logEntry);

        while (logs.size() > 1000) {
            logs.removeFirst();
        }
    }

    public void addInfoLog(String message) {
        addLog("[INFO] " + message);
    }

    public void addErrorLog(String message) {
        addLog("[ERROR] " + message);
    }

    public void addWarningLog(String message) {
        addLog("[WARNING] " + message);
    }

    public void addDebugLog(String message) {
        addLog("[DEBUG] " + message);
    }

    public synchronized String getAllLogs() {
        if (logs.isEmpty()) {
            return """
                   No hay logs disponibles.
                   
                   Este panel mostrar\u00e1 todos los logs del sistema incluyendo:
                   - Mensajes de informaci\u00f3n
                   - Errores del sistema
                   - Advertencias
                   - Logs de depuraci\u00f3n
                   - Salida de la consola
                   
                   Los logs se actualizan autom\u00e1ticamente cada 2 segundos.""";
        }

        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

    public synchronized List<String> getLogsList() {
        return new ArrayList<>(logs);
    }

    public synchronized void clearLogs() {
        logs.clear();
        addLog("[SYSTEM] Logs limpiados por el usuario");
    }

    public synchronized int getLogsCount() {
        return logs.size();
    }

    public synchronized boolean isCapturing() {
        return isCapturing;
    }

    public static void log(String message) {
        getInstance().addInfoLog(message);
    }

    public static void logError(String message) {
        getInstance().addErrorLog(message);
    }

    public static void logWarning(String message) {
        getInstance().addWarningLog(message);
    }

    public static void logDebug(String message) {
        getInstance().addDebugLog(message);
    }
}
