package com.example.interfaz.service;

import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maneja el reporte de progreso de las descargas
 * Extrae esta responsabilidad de YouTubeDownloadService
 */
public class ProgressReporter {
    
    private static final Logger LOGGER = Logger.getLogger(ProgressReporter.class.getName());
    
    private Consumer<String> progressCallback;
    private final SongFilterService songFilterService;
    
    public ProgressReporter() {
        this.songFilterService = SongFilterService.getInstance();
    }
    
    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }
    
    public void notifyProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
    }
    
    public void processDownloadLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        
        try {
            if (line.contains("[download] Downloading item")) {
                handlePlaylistProgress(line);
            } else if (line.contains("[download] 100%")) {
                handleDownloadComplete(line);
            } else if (line.contains("[ffmpeg] Destination:")) {
                handleFFmpegComplete(line);
            } else if (line.contains("[download]")) {
                handleDownloadProgress(line);
            } else if (line.contains("[ffmpeg]")) {
                handleFFmpegProgress(line);
            } else if (line.contains("has already been downloaded")) {
                handleAlreadyDownloaded(line);
            } else if (line.contains("Downloading")) {
                handleDownloadStart(line);
            }
        } catch (Exception e) {
            LOGGER.warning("Error procesando línea de progreso: " + e.getMessage());
        }
    }
    
    private void handleDownloadProgress(String line) {
        Pattern progressPattern = Pattern.compile("(\\d+\\.\\d+)%");
        Matcher progressMatcher = progressPattern.matcher(line);
        
        if (progressMatcher.find()) {
            String percentage = progressMatcher.group(1);
            notifyProgress("PROGRESS:" + percentage);
            
            Pattern speedPattern = Pattern.compile("at\\s+([\\d\\.]+\\w+/s)");
            Matcher speedMatcher = speedPattern.matcher(line);
            if (speedMatcher.find()) {
                String speed = speedMatcher.group(1);
                notifyProgress("SPEED:" + speed);
            }
            
            Pattern etaPattern = Pattern.compile("ETA\\s+([\\d:]+)");
            Matcher etaMatcher = etaPattern.matcher(line);
            if (etaMatcher.find()) {
                String eta = etaMatcher.group(1);
                notifyProgress("ETA:" + eta);
            }
        }
    }
    
    private void handleDownloadComplete(String line) {
        String fileName = extractFileNameFromProgress(line);
        if (fileName != null) {
            String songTitle = extractSongTitle(fileName);
            if (songTitle != null && !songFilterService.isDuplicateSong(songTitle)) {
                songFilterService.registerDownloadedSong(songTitle);
                notifyProgress("COMPLETED:" + songTitle);
            }
        }
        handleDownloadProgress(line);
    }
    
    private void handleFFmpegComplete(String line) {
        String fileName = extractFileNameFromFFmpeg(line);
        if (fileName != null) {
            String songTitle = extractSongTitle(fileName);
            if (songTitle != null && !songFilterService.isDuplicateSong(songTitle)) {
                songFilterService.registerDownloadedSong(songTitle);
                notifyProgress("PROCESSED:" + songTitle);
            }
            notifyProgress("PROCESSING:" + fileName);
        }
    }
    
    private void handleFFmpegProgress(String line) {
        String fileName = extractFileNameFromFFmpeg(line);
        if (fileName != null) {
            notifyProgress("PROCESSING:" + fileName);
        }
    }
    
    private void handleAlreadyDownloaded(String line) {
        String fileName = extractExistingFileName(line);
        if (fileName != null) {
            notifyProgress("ALREADY_EXISTS:" + fileName);
        }
    }
    
    private void handleDownloadStart(String line) {
        String videoTitle = extractVideoTitle(line);
        if (videoTitle != null && !videoTitle.trim().isEmpty()) {
            notifyProgress("DOWNLOADING:" + videoTitle);
        } else {
            notifyProgress("DOWNLOADING:Preparando descarga...");
        }
    }
    
    private void handlePlaylistProgress(String line) {
        Pattern playlistPattern = Pattern.compile("\\[download\\] Downloading item (\\d+) of (\\d+)");
        Matcher playlistMatcher = playlistPattern.matcher(line);
        
        if (playlistMatcher.find()) {
            String currentItem = playlistMatcher.group(1);
            String totalItems = playlistMatcher.group(2);
            
            notifyProgress("PLAYLIST_PROGRESS:" + currentItem + "/" + totalItems);
            
            notifyProgress("SONG_START:" + currentItem + "/" + totalItems);
        }
    }
    
    private String extractFileNameFromProgress(String line) {
        Pattern pattern = Pattern.compile("\\[download\\] 100% of (.+?) at");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String fullPath = matcher.group(1);
            return fullPath.substring(fullPath.lastIndexOf("\\") + 1);
        }
        return null;
    }
    
    private String extractFileNameFromFFmpeg(String line) {
        Pattern pattern = Pattern.compile("Destination: (.+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String fullPath = matcher.group(1);
            return fullPath.substring(fullPath.lastIndexOf("\\") + 1);
        }
        return null;
    }
    
    private String extractSongTitle(String fileName) {
        if (fileName == null) return null;
        
        String title = fileName;
        int lastDot = title.lastIndexOf('.');
        if (lastDot > 0) {
            title = title.substring(0, lastDot);
        }
        
        title = title.replaceAll("[\\[\\](){}]", "")
                    .replaceAll("\\s+", " ")
                    .trim();
        
        return title.isEmpty() ? null : title;
    }
    
    private String extractExistingFileName(String line) {
        Pattern pattern = Pattern.compile("\\[download\\] (.+?) has already been downloaded");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String fullPath = matcher.group(1);
            return fullPath.substring(fullPath.lastIndexOf("\\") + 1);
        }
        return null;
    }
    
    public void updateRealTimeProgress(String line) {
        processDownloadLine(line);
        
        LOGGER.info("Progreso en tiempo real: " + line);
        
        if (line.contains("[download]") || line.contains("[ffmpeg]")) {
            notifyProgress(line);
        }
    }
    
    /**
     * Extrae el título del video de una línea de descarga
     */
    private String extractVideoTitle(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        if (line.toLowerCase().contains("downloading")) {
            String[] parts = line.split("downloading", 2);
            if (parts.length > 1) {
                String title = parts[1].trim();
                title = title.replaceAll("^[:\\s]+", ""); 
                title = title.replaceAll("https?://[^\\s]+", "");
                title = title.replaceAll("\\[.*?\\]", "");
                title = title.trim();
                
                if (!title.isEmpty() && title.length() > 3) {
                    return title;
                }
            }
        }
        
        Pattern titlePattern = Pattern.compile("\\[youtube\\].*?:\\s*(.+?)(?:\\s*\\[|$)");
        Matcher titleMatcher = titlePattern.matcher(line);
        if (titleMatcher.find()) {
            String title = titleMatcher.group(1).trim();
            if (!title.isEmpty() && !title.startsWith("http")) {
                return title;
            }
        }
        
        return null;
    }
}