package com.example.interfaz.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.interfaz.service.download.ProcessExecutor;
import com.example.interfaz.service.download.SongMetadataService;
import com.example.interfaz.service.download.YtDlpCommandBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class YouTubeDownloadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testCanHandleValidYouTubeUrls() {
        try (YouTubeDownloadService service = new YouTubeDownloadService()) {
            assertTrue(service.canHandle("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            assertTrue(service.canHandle("https://youtu.be/dQw4w9WgXcQ"));
            assertTrue(service.canHandle("http://music.youtube.com/playlist?list=123"));
        }
    }

    @Test
    void testCanHandleInvalidUrls() {
        try (YouTubeDownloadService service = new YouTubeDownloadService()) {
            assertFalse(service.canHandle("https://fakeyoutube.com/watch?v=123"));
            assertFalse(service.canHandle("texto-youtube.com"));
            assertFalse(service.canHandle(null));
            assertFalse(service.canHandle(""));
        }
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenClosed() {
        YouTubeDownloadService service = new YouTubeDownloadService();
        service.close();

        assertNotNull(assertThrows(IllegalStateException.class, () -> service.downloadSong("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
        assertNotNull(assertThrows(IllegalStateException.class, () -> service.downloadPlaylist("https://www.youtube.com/playlist?list=123", "", true)));
    }

    @Test
    void downloadsPlaylistInFiftySongBatchesAndPausesOnlyBetweenDownloadedBatches() {
        RecordingProcessExecutor processExecutor = new RecordingProcessExecutor(tempDir, 120);
        RecordingYouTubeDownloadService service = new RecordingYouTubeDownloadService(processExecutor);
        List<File> completed = new ArrayList<>();
        List<String> progress = new ArrayList<>();
        service.setProgressCallback(progress::add);

        try (service) {
            assertTrue(service.downloadRawAudioStreaming(
                    "https://www.youtube.com/playlist?list=batch-test",
                    tempDir.toString(),
                    completed::add));
        }

        assertEquals(3, processExecutor.downloadCommands.size());
        assertRange(processExecutor.downloadCommands.get(0), "1", "50");
        assertRange(processExecutor.downloadCommands.get(1), "51", "100");
        assertRange(processExecutor.downloadCommands.get(2), "101", "120");
        assertEquals(List.of(45L, 45L), service.waits);
        assertEquals(3, completed.size());
        assertTrue(progress.contains("PLAYLIST_PROGRESS:51/120"));
        assertTrue(progress.contains("PLAYLIST_PROGRESS:101/120"));
    }

    @Test
    void unavailableVideoDoesNotStopTheFollowingPlaylistBatch() {
        UnavailableItemProcessExecutor processExecutor = new UnavailableItemProcessExecutor(tempDir, 75);
        RecordingYouTubeDownloadService service = new RecordingYouTubeDownloadService(processExecutor);

        try (service) {
            assertTrue(service.downloadRawAudioStreaming(
                    "https://www.youtube.com/playlist?list=unavailable-test",
                    tempDir.toString(),
                    file -> { }));
        }

        assertEquals(2, processExecutor.downloadCommands.size());
        assertRange(processExecutor.downloadCommands.get(0), "1", "50");
        assertRange(processExecutor.downloadCommands.get(1), "51", "75");
        assertEquals(List.of(45L), service.waits);
    }

    @Test
    void rateLimitStopsAfterThreeAutomaticAttempts() {
        RateLimitedProcessExecutor processExecutor = new RateLimitedProcessExecutor(10, 10);
        RecordingYouTubeDownloadService service = new RecordingYouTubeDownloadService(processExecutor);

        try (service) {
            assertFalse(service.downloadRawAudioStreaming(
                    "https://www.youtube.com/playlist?list=rate-limit-test",
                    tempDir.toString(),
                    file -> { }));
        }

        assertEquals(3, processExecutor.downloadAttempts);
        assertEquals(List.of(30L, 120L), service.waits);
    }

    @Test
    void automaticRetryDelayIsProgressiveAndCapped() {
        assertEquals(15L, YouTubeDownloadService.automaticRetryDelaySeconds(1, false));
        assertEquals(30L, YouTubeDownloadService.automaticRetryDelaySeconds(2, false));
        assertEquals(300L, YouTubeDownloadService.automaticRetryDelaySeconds(10, false));
        assertEquals(30L, YouTubeDownloadService.automaticRetryDelaySeconds(1, true));
        assertEquals(120L, YouTubeDownloadService.automaticRetryDelaySeconds(2, true));
        assertEquals(300L, YouTubeDownloadService.automaticRetryDelaySeconds(3, true));
        assertEquals(600L, YouTubeDownloadService.automaticRetryDelaySeconds(4, true));
    }

    private static void assertRange(List<String> command, String expectedStart, String expectedEnd) {
        assertEquals(expectedStart, valueAfter(command, "--playlist-start"));
        assertEquals(expectedEnd, valueAfter(command, "--playlist-end"));
        assertFalse(command.contains("--sleep-requests"));
    }

    private static String valueAfter(List<String> command, String option) {
        int index = command.indexOf(option);
        assertTrue(index >= 0 && index + 1 < command.size());
        return command.get(index + 1);
    }

    private static final class RecordingYouTubeDownloadService extends YouTubeDownloadService {
        private final List<Long> waits = new ArrayList<>();

        private RecordingYouTubeDownloadService(ProcessExecutor processExecutor) {
            super(new ProgressReporter(), new YtDlpCommandBuilder(), processExecutor, new SongMetadataService());
        }

        @Override
        protected void waitInterruptibly(long seconds) {
            waits.add(seconds);
        }
    }

    private static final class RecordingProcessExecutor extends ProcessExecutor {
        private final Path stagingDir;
        private final int playlistSize;
        private final List<List<String>> downloadCommands = new ArrayList<>();

        private RecordingProcessExecutor(Path stagingDir, int playlistSize) {
            this.stagingDir = stagingDir;
            this.playlistSize = playlistSize;
        }

        @Override
        public boolean execute(List<String> command, Consumer<String> lineProcessor,
                Consumer<String> logNotifier) throws IOException {
            if (command.contains("--flat-playlist")) {
                for (int i = 0; i < playlistSize; i++) {
                    lineProcessor.accept(String.format("id%09d", i));
                }
                return true;
            }

            downloadCommands.add(List.copyOf(command));
            String start = valueAfter(command, "--playlist-start");
            String end = valueAfter(command, "--playlist-end");
            int localTotal = Integer.parseInt(end) - Integer.parseInt(start) + 1;
            Path rawFile = stagingDir.resolve("raw_" + start + "___Song.webm");
            Files.writeString(rawFile, "audio");
            lineProcessor.accept("[download] Downloading item 1 of " + localTotal);
            lineProcessor.accept("[download] Destination: " + rawFile);
            lineProcessor.accept("[download] 100% of 1.00MiB");
            return true;
        }
    }

    private static final class UnavailableItemProcessExecutor extends ProcessExecutor {
        private final Path stagingDir;
        private final int playlistSize;
        private final List<List<String>> downloadCommands = new ArrayList<>();

        private UnavailableItemProcessExecutor(Path stagingDir, int playlistSize) {
            this.stagingDir = stagingDir;
            this.playlistSize = playlistSize;
        }

        @Override
        public boolean execute(List<String> command, Consumer<String> lineProcessor,
                Consumer<String> logNotifier) throws IOException {
            if (command.contains("--flat-playlist")) {
                emitPlaylistIds(lineProcessor, playlistSize);
                return true;
            }

            downloadCommands.add(List.copyOf(command));
            String start = valueAfter(command, "--playlist-start");
            if ("1".equals(start)) {
                Path rawFile = stagingDir.resolve("raw_available___Song.webm");
                Files.writeString(rawFile, "audio");
                lineProcessor.accept("[download] Destination: " + rawFile);
                lineProcessor.accept("[download] 100% of 1.00MiB");
                lineProcessor.accept("ERROR: [youtube] unavailable01: This video is no longer available because the YouTube account associated with this video has been terminated.");
                lineProcessor.accept("[download] Finished downloading playlist: Test");
                return false;
            }
            return true;
        }
    }

    private static final class RateLimitedProcessExecutor extends ProcessExecutor {
        private final int playlistSize;
        private final int failuresBeforeSuccess;
        private int downloadAttempts;

        private RateLimitedProcessExecutor(int playlistSize, int failuresBeforeSuccess) {
            this.playlistSize = playlistSize;
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public boolean execute(List<String> command, Consumer<String> lineProcessor,
                Consumer<String> logNotifier) {
            if (command.contains("--flat-playlist")) {
                emitPlaylistIds(lineProcessor, playlistSize);
                return true;
            }

            downloadAttempts++;
            if (downloadAttempts <= failuresBeforeSuccess) {
                lineProcessor.accept("ERROR: The current session has been rate-limited by YouTube");
                return false;
            }
            return true;
        }
    }

    private static void emitPlaylistIds(Consumer<String> lineProcessor, int playlistSize) {
        for (int i = 0; i < playlistSize; i++) {
            lineProcessor.accept(String.format("id%09d", i));
        }
    }
}
