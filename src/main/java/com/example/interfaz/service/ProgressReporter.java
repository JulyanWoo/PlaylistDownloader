package com.example.interfaz.service;

import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses yt-dlp stdout lines and forwards structured progress events.
 * Does NOT register song titles, write to any file, or maintain history.
 */
public class ProgressReporter {

    private static final Logger LOGGER = Logger.getLogger(ProgressReporter.class.getName());

    private Consumer<String> progressCallback;

    public ProgressReporter() {}

    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    public void notifyProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
    }

    /** Processes one line of yt-dlp output and fires the appropriate event. */
    public void processDownloadLine(String line) {
        if (line == null || line.trim().isEmpty()) return;

        try {
            if (line.contains("[download] Downloading item")) {
                handlePlaylistProgress(line);
            } else if (line.contains("[download]")) {
                handleDownloadProgress(line);
            } else if (line.contains("[ffmpeg]")) {
                handleFFmpegProgress(line);
            }
        } catch (Exception e) {
            LOGGER.warning(() -> "Error processing progress line: " + e.getMessage());
        }
    }

    private void handlePlaylistProgress(String line) {
        Pattern p = Pattern.compile("\\[download\\] Downloading item (\\d+) of (\\d+)");
        Matcher m = p.matcher(line);
        if (m.find()) {
            notifyProgress("PLAYLIST_PROGRESS:" + m.group(1) + "/" + m.group(2));
            notifyProgress("SONG_START:" + m.group(1) + "/" + m.group(2));
        }
    }

    private void handleDownloadProgress(String line) {
        // Percentage
        Pattern progressP = Pattern.compile("(\\d+\\.\\d+)%");
        Matcher progressM = progressP.matcher(line);
        if (progressM.find()) {
            notifyProgress("PROGRESS:" + progressM.group(1));
        }

        // Speed
        Pattern speedP = Pattern.compile("at\\s+([\\d.]+\\w+/s)");
        Matcher speedM = speedP.matcher(line);
        if (speedM.find()) {
            notifyProgress("SPEED:" + speedM.group(1));
        }

        // ETA
        Pattern etaP = Pattern.compile("ETA\\s+([\\d:]+)");
        Matcher etaM = etaP.matcher(line);
        if (etaM.find()) {
            notifyProgress("ETA:" + etaM.group(1));
        }

        // Already downloaded notice — just a status message, no file registration
        if (line.contains("has already been downloaded")) {
            notifyProgress("ALREADY_EXISTS");
        }
    }

    private void handleFFmpegProgress(String line) {
        if (line.contains("Destination:")) {
            notifyProgress("PROCESSING");
        }
    }
}
