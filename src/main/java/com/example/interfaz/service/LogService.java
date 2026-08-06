package com.example.interfaz.service;

import com.example.interfaz.model.LogEntry;
import com.example.interfaz.model.LogEntry.LogLevel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class LogService {
    private static LogService instance;
    private final Deque<LogEntry> logs;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private boolean isCapturing = false;

    private LogService() {
        logs = new ArrayDeque<>();
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
                            addLog(LogLevel.INFO, line);
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
                            addLog(LogLevel.ERROR, line);
                        }
                        buffer.setLength(0);
                    } else {
                        buffer.append(c);
                    }
                }
            }, true);

            System.setOut(customOut);
            System.setErr(customErr);

            addLog(LogLevel.SYSTEM, "LogService iniciado - Captura de logs activada");
        }
    }

    public synchronized void stopCapturing() {
        if (isCapturing) {
            isCapturing = false;
            System.setOut(originalOut);
            System.setErr(originalErr);
            addLog(LogLevel.SYSTEM, "LogService detenido - Captura de logs desactivada");
        }
    }

    public synchronized void addLog(LogLevel level, String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), level, message);
        logs.addLast(entry);

        while (logs.size() > 1000) {
            logs.removeFirst();
        }
    }

    public synchronized void addLog(String message) {
        addLog(LogLevel.INFO, message);
    }

    public void addInfoLog(String message) {
        addLog(LogLevel.INFO, message);
    }

    public void addErrorLog(String message) {
        addLog(LogLevel.ERROR, message);
    }

    public void addWarningLog(String message) {
        addLog(LogLevel.WARNING, message);
    }

    public void addDebugLog(String message) {
        addLog(LogLevel.DEBUG, message);
    }

    public synchronized List<LogEntry> getLogEntries() {
        return new ArrayList<>(logs);
    }

    public synchronized String getAllLogs() {
        if (logs.isEmpty()) {
            return "No hay logs disponibles.";
        }

        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : logs) {
            sb.append(entry.getFormattedMessage()).append("\n");
        }
        return sb.toString();
    }

    public synchronized List<String> getLogsList() {
        List<String> list = new ArrayList<>(logs.size());
        for (LogEntry entry : logs) {
            list.add(entry.getFormattedMessage());
        }
        return list;
    }

    public synchronized void clearLogs() {
        logs.clear();
        addLog(LogLevel.SYSTEM, "Logs limpiados por el usuario");
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
