package com.example.interfaz.event;

import com.example.interfaz.model.Song;

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

    public static class DownloadStarted extends DownloadEvent {

        public DownloadStarted(Song song) {
            super(song);
        }
    }

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

    public static class StateChanged extends DownloadEvent {

        private final boolean isDownloading;
        private final boolean isPaused;

        public StateChanged(boolean isDownloading, boolean isPaused) {
            super(null);
            this.isDownloading = isDownloading;
            this.isPaused = isPaused;
        }

        public boolean isDownloading() {
            return isDownloading;
        }

        public boolean isPaused() {
            return isPaused;
        }
    }

    public static class QueueEmpty extends DownloadEvent {

        public QueueEmpty() {
            super(null);
        }
    }

    public static class QueueUpdated extends DownloadEvent {

        public QueueUpdated() {
            super(null);
        }
    }
}
