package com.example.interfaz.event.analyzer;

import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.LibraryAnalysisResult;

public abstract class LibraryAnalyzerEvent {

    private final long timestamp;

    protected LibraryAnalyzerEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static class LibraryAnalysisStarted extends LibraryAnalyzerEvent {
        private final String folderPath;

        public LibraryAnalysisStarted(String folderPath) {
            this.folderPath = folderPath;
        }

        public String getFolderPath() {
            return folderPath;
        }
    }

    public static class LibraryProgressUpdated extends LibraryAnalyzerEvent {
        private final long processedSongs;
        private final long totalFiles;
        private final long duplicatesFound;
        private final String statusText;
        private final long elapsedMillis;
        private final long estimatedRemainingMillis;

        public LibraryProgressUpdated(long processedSongs, long totalFiles, long duplicatesFound,
                                      String statusText, long elapsedMillis, long estimatedRemainingMillis) {
            this.processedSongs = processedSongs;
            this.totalFiles = totalFiles;
            this.duplicatesFound = duplicatesFound;
            this.statusText = statusText;
            this.elapsedMillis = elapsedMillis;
            this.estimatedRemainingMillis = estimatedRemainingMillis;
        }

        public long getProcessedSongs() {
            return processedSongs;
        }

        public long getTotalFiles() {
            return totalFiles;
        }

        public long getDuplicatesFound() {
            return duplicatesFound;
        }

        public String getStatusText() {
            return statusText;
        }

        public long getElapsedMillis() {
            return elapsedMillis;
        }

        public long getEstimatedRemainingMillis() {
            return estimatedRemainingMillis;
        }
    }

    public static class DuplicateFound extends LibraryAnalyzerEvent {
        private final DuplicateGroup group;

        public DuplicateFound(DuplicateGroup group) {
            this.group = group;
        }

        public DuplicateGroup getGroup() {
            return group;
        }
    }

    public static class LibraryAnalysisFinished extends LibraryAnalyzerEvent {
        private final LibraryAnalysisResult result;

        public LibraryAnalysisFinished(LibraryAnalysisResult result) {
            this.result = result;
        }

        public LibraryAnalysisResult getResult() {
            return result;
        }
    }

    public static class LibraryAnalysisCancelled extends LibraryAnalyzerEvent {
        private final LibraryAnalysisResult partialResult;

        public LibraryAnalysisCancelled(LibraryAnalysisResult partialResult) {
            this.partialResult = partialResult;
        }

        public LibraryAnalysisResult getPartialResult() {
            return partialResult;
        }
    }
}
