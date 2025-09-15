package com.example.interfaz.event;

import com.example.interfaz.model.Song;

/**
 * Eventos relacionados con descargas
 */
public abstract class DownloadEvent {
    
    private final Song song;
    private final long timestamp;
    
    protected DownloadEvent(Song song) {
        this.song = song;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Song getSong() {
        return song;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Evento cuando se inicia una descarga
     */
    public static class DownloadStarted extends DownloadEvent {
        public DownloadStarted(Song song) {
            super(song);
        }
    }
    
    /**
     * Evento cuando se completa una descarga
     */
    public static class DownloadCompleted extends DownloadEvent {
        private final String filePath;
        
        public DownloadCompleted(Song song, String filePath) {
            super(song);
            this.filePath = filePath;
        }
        
        public String getFilePath() {
            return filePath;
        }
    }
    
    /**
     * Evento cuando falla una descarga
     */
    public static class DownloadFailed extends DownloadEvent {
        private final String error;
        
        public DownloadFailed(Song song, String error) {
            super(song);
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Evento de progreso de descarga
     */
    public static class DownloadProgress extends DownloadEvent {
        private final double progress;
        private final String status;
        
        public DownloadProgress(Song song, double progress, String status) {
            super(song);
            this.progress = progress;
            this.status = status;
        }
        
        public double getProgress() {
            return progress;
        }
        
        public String getStatus() {
            return status;
        }
    }
}