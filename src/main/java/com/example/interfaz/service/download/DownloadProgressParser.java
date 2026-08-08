package com.example.interfaz.service.download;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadProgressParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadProgressParser.class);

    private volatile ProgressListener listener;
    private final Map<String, BiConsumer<String, ProgressListener>> handlers = new HashMap<>();

    public interface ProgressListener {

        void onOverallProgress(int current, int total);

        void onSongStart(String songTitle);

        void onCurrentProgress(double progress, String statusText);

        void onSpeedUpdate(String speed);

        void onEtaUpdate(String eta);

        void onStatusUpdate(String statusMessage);

        void onGenericMessage(String message);
    }

    public DownloadProgressParser() {
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put("PLAYLIST_PROGRESS:", this::handlePlaylistProgress);
        handlers.put("SONG_START:", this::handleSongStart);
        handlers.put("PROGRESS:", this::handleCurrentProgress);
        handlers.put("SPEED:", this::handleSpeedUpdate);
        handlers.put("ETA:", this::handleEtaUpdate);
        handlers.put("DOWNLOADING:", this::handleDownloading);
        handlers.put("COMPLETED:", this::handleCompleted);
        handlers.put("PROCESSED:", this::handleCompleted);
    }

    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void parseAndDispatch(String message) {
        parseAndDispatch(message, this.listener);
    }

    public void parseAndDispatch(String message, ProgressListener customListener) {
        ProgressListener target = customListener != null ? customListener : this.listener;
        if (message == null || target == null) {
            return;
        }

        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        for (Map.Entry<String, BiConsumer<String, ProgressListener>> entry : handlers.entrySet()) {
            String prefix = entry.getKey();
            if (trimmed.startsWith(prefix)) {
                String payload = trimmed.substring(prefix.length()).trim();
                entry.getValue().accept(payload, target);
                return;
            }
        }

        if (!isIgnoredLogLine(trimmed)) {
            target.onGenericMessage(trimmed);
        }
    }

    private void handlePlaylistProgress(String payload, ProgressListener target) {
        String[] parts = payload.split("/", 2);
        if (parts.length == 2) {
            try {
                int currentItem = Integer.parseInt(parts[0].trim());
                int totalItems = Integer.parseInt(parts[1].trim());
                target.onOverallProgress(currentItem, totalItems);
            } catch (NumberFormatException e) {
                LOGGER.warn("Error parseando número en progreso de playlist: {}", payload);
            }
        }
    }

    private void handleSongStart(String payload, ProgressListener target) {
        target.onSongStart(payload);
    }

    private void handleCurrentProgress(String payload, ProgressListener target) {
        try {
            double percentage = Double.parseDouble(payload);
            double progress = percentage / 100.0;
            target.onCurrentProgress(progress, String.format("%.1f%%", percentage));
        } catch (NumberFormatException e) {
            LOGGER.warn("Error parseando porcentaje de progreso: {}", payload);
        }
    }

    private void handleSpeedUpdate(String payload, ProgressListener target) {
        target.onSpeedUpdate(payload);
    }

    private void handleEtaUpdate(String payload, ProgressListener target) {
        target.onEtaUpdate(payload);
    }

    private void handleDownloading(String payload, ProgressListener target) {
        target.onSongStart(payload);
        target.onStatusUpdate(payload);
    }

    private void handleCompleted(String payload, ProgressListener target) {
        target.onStatusUpdate(payload);
    }

    private boolean isIgnoredLogLine(String message) {
        return message.startsWith("DOWNLOAD_START:")
                || message.contains("[youtube:tab]")
                || message.contains("[youtube]")
                || message.contains("[download]")
                || message.startsWith("Iniciando descarga")
                || message.contains("Downloading item")
                || message.contains("API JSON")
                || message.contains("player API")
                || message.contains("ios player");
    }
}
