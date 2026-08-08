package com.example.interfaz.service.update;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCache.class);
    private static final String DEFAULT_CONFIG_DIR = "config";
    private static final String UPDATE_FILE_NAME = "update.json";
    private final Path configFilePath;

    public UpdateCache() {
        this(Path.of(DEFAULT_CONFIG_DIR, UPDATE_FILE_NAME));
    }

    public UpdateCache(Path configFilePath) {
        this.configFilePath = configFilePath;
    }

    public boolean isCacheValidHours(int maxHours) {
        LocalDateTime lastCheck = getLastCheckTime();
        if (lastCheck == null) {
            return false;
        }
        long hours = ChronoUnit.HOURS.between(lastCheck, LocalDateTime.now());
        return hours >= 0 && hours < maxHours;
    }

    public LocalDateTime getLastCheckTime() {
        if (!Files.exists(configFilePath)) {
            return null;
        }
        try {
            String json = Files.readString(configFilePath, StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("\"lastCheck\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return LocalDateTime.parse(matcher.group(1).trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (java.io.IOException | java.time.format.DateTimeParseException e) {
            LOGGER.debug("No se pudo leer lastCheck desde {}: {}", configFilePath, e.getMessage());
        }
        return null;
    }

    public UpdateInfo getCachedUpdateInfo(String currentVersion) {
        if (!Files.exists(configFilePath)) {
            return null;
        }
        try {
            String json = Files.readString(configFilePath, StandardCharsets.UTF_8);

            String lastVersion = extractJsonField(json, "lastVersion");
            String downloadUrl = extractJsonField(json, "downloadUrl");
            String updateAvailStr = extractJsonField(json, "updateAvailable");
            boolean updateAvailable = Boolean.parseBoolean(updateAvailStr);

            if (lastVersion.isBlank()) {
                return null;
            }

            return new UpdateInfo(currentVersion, lastVersion, updateAvailable, downloadUrl);
        } catch (java.io.IOException e) {
            LOGGER.warn("Error leyendo cache de actualización desde {}: {}", configFilePath, e.getMessage());
            return null;
        }
    }

    public void saveCache(UpdateInfo info) {
        if (info == null) {
            return;
        }
        try {
            Path parent = configFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String json = String.format("""
                {
                  "lastCheck": "%s",
                  "lastVersion": "%s",
                  "updateAvailable": %b,
                  "downloadUrl": "%s"
                }
                """,
                    nowStr,
                    escapeJson(info.getLatestVersion()),
                    info.isUpdateAvailable(),
                    escapeJson(info.getDownloadUrl())
            );

            Files.writeString(configFilePath, json, StandardCharsets.UTF_8);
            LOGGER.info("Caché de actualización guardado en {}", configFilePath);
        } catch (java.io.IOException e) {
            LOGGER.error("No se pudo guardar el caché de actualización en {}", configFilePath, e);
        }
    }

    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(?:\"([^\"]*)\"|(true|false))");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(2).trim();
        }
        return "";
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
