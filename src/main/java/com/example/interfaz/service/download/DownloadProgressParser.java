package com.example.interfaz.service.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadProgressParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadProgressParser.class);

    public interface ProgressListener {
        void onOverallProgress(int current, int total);
        void onSongStart(String songTitle);
        void onCurrentProgress(double progress, String statusText);
        void onSpeedUpdate(String speed);
        void onEtaUpdate(String eta);
        void onStatusUpdate(String statusMessage);
        void onGenericMessage(String message);
    }

    public void parseAndDispatch(String message, ProgressListener listener) {
        if (message == null || listener == null) return;

        if (message.startsWith("PLAYLIST_PROGRESS:")) {
            String progressInfo = message.substring("PLAYLIST_PROGRESS:".length());
            String[] parts = progressInfo.split("/");
            if (parts.length == 2) {
                try {
                    int currentItem = Integer.parseInt(parts[0]);
                    int totalItems = Integer.parseInt(parts[1]);
                    listener.onOverallProgress(currentItem, totalItems);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Error parseando progreso de playlist: {}", message);
                }
            }
        } else if (message.startsWith("SONG_START:")) {
            String songInfo = message.substring("SONG_START:".length());
            listener.onSongStart(songInfo);
        } else if (message.startsWith("PROGRESS:")) {
            String percentageStr = message.substring("PROGRESS:".length());
            try {
                double percentage = Double.parseDouble(percentageStr);
                double progress = percentage / 100.0;
                listener.onCurrentProgress(progress, String.format("Descargando... %.1f%%", percentage));
            } catch (NumberFormatException e) {
                LOGGER.warn("Error parseando porcentaje: {}", message);
            }
        } else if (message.startsWith("SPEED:")) {
            String speed = message.substring("SPEED:".length());
            listener.onSpeedUpdate(speed);
        } else if (message.startsWith("ETA:")) {
            String eta = message.substring("ETA:".length());
            listener.onEtaUpdate(eta);
        } else if (message.startsWith("DOWNLOADING:")) {
            String videoTitle = message.substring("DOWNLOADING:".length());
            listener.onSongStart(videoTitle);
            listener.onStatusUpdate("🎵 Descargando...");
        } else if (message.startsWith("COMPLETED:") || message.startsWith("PROCESSED:")) {
            String completedTitle = message.substring(message.indexOf(":") + 1);
            listener.onStatusUpdate("✅ Completado: " + completedTitle);
        } else if (!isIgnoredLogLine(message)) {
            listener.onGenericMessage(message);
        }
    }

    private boolean isIgnoredLogLine(String message) {
        return message.startsWith("DOWNLOAD_START:") ||
               message.contains("[youtube:tab]") ||
               message.contains("[youtube]") ||
               message.contains("[download]") ||
               message.startsWith("Iniciando descarga") ||
               message.contains("Downloading item") ||
               message.contains("API JSON") ||
               message.contains("player API") ||
               message.contains("ios player");
    }
}
