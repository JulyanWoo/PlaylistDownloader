package com.example.interfaz.service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogService {
    private static LogService instance;
    private final List<String> logs;
    private final DateTimeFormatter formatter;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private boolean isCapturing = false;

    private LogService() {
        logs = new CopyOnWriteArrayList<>();
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

    public void startCapturing() {
        if (!isCapturing) {
            isCapturing = true;
            
            PrintStream customOut = new PrintStream(new OutputStream() {
                private StringBuilder buffer = new StringBuilder();
                
                @Override
                public void write(int b) throws IOException {
                    originalOut.write(b);
                    
                    char c = (char) b;
                    if (c == '\n') {
                        String line = buffer.toString();
                        if (!line.trim().isEmpty()) {
                            addLog("[INFO] " + line);
                        }
                        buffer = new StringBuilder();
                    } else {
                        buffer.append(c);
                    }
                }
            });
            
            PrintStream customErr = new PrintStream(new OutputStream() {
                private StringBuilder buffer = new StringBuilder();
                
                @Override
                public void write(int b) throws IOException {
                    originalErr.write(b);
                    
                    char c = (char) b;
                    if (c == '\n') {
                        String line = buffer.toString();
                        if (!line.trim().isEmpty()) {
                            addLog("[ERROR] " + line);
                        }
                        buffer = new StringBuilder();
                    } else {
                        buffer.append(c);
                    }
                }
            });
            
            System.setOut(customOut);
            System.setErr(customErr);
            
            addLog("[SYSTEM] LogService iniciado - Captura de logs activada");
        }
    }

    public void stopCapturing() {
        if (isCapturing) {
            isCapturing = false;
            System.setOut(originalOut);
            System.setErr(originalErr);
            addLog("[SYSTEM] LogService detenido - Captura de logs desactivada");
        }
    }

    public void addLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] %s", timestamp, message);
        logs.add(logEntry);
        
        if (logs.size() > 1000) {
            logs.remove(0);
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

    public String getAllLogs() {
        if (logs.isEmpty()) {
            return "No hay logs disponibles.\n\nEste panel mostrará todos los logs del sistema incluyendo:\n" +
                   "- Mensajes de información\n" +
                   "- Errores del sistema\n" +
                   "- Advertencias\n" +
                   "- Logs de depuración\n" +
                   "- Salida de la consola\n\n" +
                   "Los logs se actualizan automáticamente cada 2 segundos.";
        }
        
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        return sb.toString();
    }

    public List<String> getLogsList() {
        return new ArrayList<>(logs);
    }

    public void clearLogs() {
        logs.clear();
        addLog("[SYSTEM] Logs limpiados por el usuario");
    }

    public int getLogsCount() {
        return logs.size();
    }

    public boolean isCapturing() {
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