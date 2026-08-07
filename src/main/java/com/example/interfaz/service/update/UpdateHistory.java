package com.example.interfaz.service.update;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHistory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateHistory.class);
    private static final String DEFAULT_CONFIG_DIR = "config";
    private static final String HISTORY_FILE_NAME = "update-history.json";
    private final Path historyFilePath;

    public record Entry(String fromVersion, String toVersion, String timestamp, boolean success) {}

    public UpdateHistory() {
        this(Path.of(DEFAULT_CONFIG_DIR, HISTORY_FILE_NAME));
    }

    public UpdateHistory(Path historyFilePath) {
        this.historyFilePath = historyFilePath;
    }

    public synchronized void recordUpdate(String fromVersion, String toVersion, boolean success) {
        List<Entry> entries = loadHistory();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        entries.add(new Entry(fromVersion != null ? fromVersion : "Desconocida",
                toVersion != null ? toVersion : "Desconocida",
                timestamp, success));

        saveHistory(entries);
    }

    public synchronized List<Entry> loadHistory() {
        List<Entry> entries = new ArrayList<>();
        if (!Files.exists(historyFilePath)) {
            return entries;
        }

        try {
            String json = Files.readString(historyFilePath, StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("\\{\\s*\"from\"\\s*:\\s*\"([^\"]*)\",\\s*\"to\"\\s*:\\s*\"([^\"]*)\",\\s*\"date\"\\s*:\\s*\"([^\"]*)\",\\s*\"success\"\\s*:\\s*(true|false)\\s*\\}");
            Matcher matcher = pattern.matcher(json);
            while (matcher.find()) {
                entries.add(new Entry(
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        Boolean.parseBoolean(matcher.group(4))
                ));
            }
        } catch (Exception e) {
            LOGGER.warn("Error leyendo historial de actualizaciones desde {}: {}", historyFilePath, e.getMessage());
        }
        return entries;
    }

    private void saveHistory(List<Entry> entries) {
        try {
            Path parent = historyFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                sb.append(String.format("  {\n    \"from\": \"%s\",\n    \"to\": \"%s\",\n    \"date\": \"%s\",\n    \"success\": %b\n  }",
                        escapeJson(e.fromVersion()),
                        escapeJson(e.toVersion()),
                        escapeJson(e.timestamp()),
                        e.success()
                ));
                if (i < entries.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("]\n");

            Files.writeString(historyFilePath, sb.toString(), StandardCharsets.UTF_8);
            LOGGER.info("Historial de actualizaciones guardado en {}", historyFilePath);
        } catch (Exception e) {
            LOGGER.error("No se pudo guardar el historial de actualizaciones en {}", historyFilePath, e);
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
