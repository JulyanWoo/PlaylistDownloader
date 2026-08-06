package com.example.interfaz.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {

    public enum LogLevel {
        INFO,
        ERROR,
        WARNING,
        DEBUG,
        SYSTEM
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final LogLevel level;
    private final String message;

    public LogEntry(LocalDateTime timestamp, LogLevel level, String message) {
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.level = level != null ? level : LogLevel.INFO;
        this.message = message != null ? message : "";
    }

    public LogEntry(LogLevel level, String message) {
        this(LocalDateTime.now(), level, message);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedMessage() {
        return String.format("[%s] [%s] %s", timestamp.format(FORMATTER), level.name(), message);
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
