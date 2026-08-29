package com.example.interfaz.service.download;

import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.event.EventBus;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.YouTubeDownloadService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DownloadCoordinatorTest {

    @Test
    void testDownloadCoordinatorQueueOperations() {
        DownloadService downloadService = new YouTubeDownloadService();
        QueueManager queueManager = new QueueManager();
        EventBus eventBus = new EventBus();

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus)) {
            boolean added = coordinator.addToQueue("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            assertTrue(added);

            boolean removed = coordinator.removeFromQueue("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            assertTrue(removed);

            coordinator.clearQueue();
            assertTrue(queueManager.isEmpty());
        }
    }

    @Test
    void shouldWorkWithAnyGenericDownloadService() throws InterruptedException {
        QueueManager queueManager = new QueueManager();
        EventBus eventBus = new EventBus();
        AtomicBoolean downloadCompletedCalled = new AtomicBoolean(false);

        eventBus.subscribe(DownloadEvent.DownloadCompleted.class, event -> downloadCompletedCalled.set(true));

        DownloadService mockService = new DownloadService() {
            @Override
            public CompletableFuture<Boolean> downloadSong(String url, String outputPath) {
                return CompletableFuture.completedFuture(true);
            }
            @Override public boolean canHandle(String url) { return true; }
            @Override public Song getSongInfo(String url) { Song s = new Song(); s.setTitle("Test Song"); return s; }
            @Override public void pauseDownload() {}
            @Override public void resumeDownload() {}
            @Override public void stopDownload() {}
            @Override public void close() {}
        };

        try (DownloadCoordinator coordinator = new DownloadCoordinator(mockService, queueManager, eventBus)) {
            coordinator.addToQueue("https://example.com/test");
            coordinator.startDownload();

            Thread.sleep(300);
            assertTrue(downloadCompletedCalled.get(), "Cualquier DownloadService genérico debe notificar DownloadCompletedEvent");
        }
    }

    @Test
    void shouldMarkFailedWhenServiceThrowsException() throws InterruptedException {
        QueueManager queueManager = new QueueManager();
        EventBus eventBus = new EventBus();
        AtomicBoolean failedCalled = new AtomicBoolean(false);

        eventBus.subscribe(DownloadEvent.DownloadFailed.class, event -> failedCalled.set(true));

        DownloadService failingService = new DownloadService() {
            @Override
            public CompletableFuture<Boolean> downloadSong(String url, String outputPath) {
                return CompletableFuture.failedFuture(new RuntimeException("Simulated Network Error"));
            }
            @Override public boolean canHandle(String url) { return true; }
            @Override public Song getSongInfo(String url) { return new Song(); }
            @Override public void pauseDownload() {}
            @Override public void resumeDownload() {}
            @Override public void stopDownload() {}
            @Override public void close() {}
        };

        try (DownloadCoordinator coordinator = new DownloadCoordinator(failingService, queueManager, eventBus)) {
            coordinator.addToQueue("https://example.com/fail");
            coordinator.startDownload();

            Thread.sleep(300);
            assertTrue(failedCalled.get());
            assertEquals(1, queueManager.getFailedCount());
        }
    }

    @Test
    void shouldUnsubscribeListenersOnClose() {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();
        DownloadService downloadService = new YouTubeDownloadService();

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus)) {
            assertNotNull(coordinator);
            int initialStartedListeners = eventBus.getListenerCount(DownloadEvent.DownloadStarted.class);
            assertTrue(initialStartedListeners > 0);
        }

        assertEquals(0, eventBus.getListenerCount(DownloadEvent.DownloadStarted.class), "listeners deben desuscribirse al cerrar coordinator");
    }

    @Test
    void shouldNotStartTwoDownloadsAtSameTime() throws InterruptedException {
        QueueManager queueManager = new QueueManager();
        EventBus eventBus = new EventBus();
        AtomicInteger downloadCallCount = new AtomicInteger(0);

        DownloadService slowService = new DownloadService() {
            @Override
            public CompletableFuture<Boolean> downloadSong(String url, String outputPath) {
                downloadCallCount.incrementAndGet();
                return CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    return true;
                });
            }
            @Override public boolean canHandle(String url) { return true; }
            @Override public Song getSongInfo(String url) { return new Song(); }
            @Override public void pauseDownload() {}
            @Override public void resumeDownload() {}
            @Override public void stopDownload() {}
            @Override public void close() {}
        };

        try (DownloadCoordinator coordinator = new DownloadCoordinator(slowService, queueManager, eventBus)) {
            coordinator.addToQueue("https://example.com/slow1");
            coordinator.addToQueue("https://example.com/slow2");

            coordinator.startDownload();
            assertTrue(coordinator.isDownloading());

            coordinator.startDownload();

            Thread.sleep(1200);
            assertEquals(2, downloadCallCount.get(), "Ambas canciones deben ser procesadas secuencialmente en un único ciclo de descarga");
        }
    }

    @Test
    void shouldShutdownExecutorAndCleanState() {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();
        DownloadService downloadService = new YouTubeDownloadService();

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus)) {
            coordinator.addToQueue("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            coordinator.startDownload();

            coordinator.shutdown();

            assertFalse(coordinator.isDownloading(), "isDownloading() debe retornar false tras shutdown()");
            assertEquals(0, eventBus.getListenerCount(DownloadEvent.DownloadStarted.class));
        }
    }

    @Test
    void shouldBeIdempotentOnMultipleCancelAndShutdown() {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();
        DownloadService downloadService = new YouTubeDownloadService();

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus)) {
            assertDoesNotThrow(() -> {
                coordinator.cancelDownload();
                coordinator.cancelDownload();
                coordinator.shutdown();
                coordinator.shutdown();
            });

            assertNotNull(assertThrows(IllegalStateException.class, coordinator::startDownload));
            assertNotNull(assertThrows(IllegalStateException.class, () -> coordinator.addToQueue("https://youtube.com/watch?v=1")));
        }
    }

    @Test
    void testPipelineModeExecution(@org.junit.jupiter.api.io.TempDir java.io.File tempDir) throws Exception {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();

        java.io.File mockRaw = new java.io.File(tempDir, "mock.webm");
        java.nio.file.Files.writeString(mockRaw.toPath(), "raw-stream");

        YouTubeDownloadService mockYtService = new YouTubeDownloadService() {
            @Override
            public boolean canHandle(String url) { return true; }
            @Override
            public Song getSongInfo(String url) { Song s = new Song(); s.setTitle("Pipeline Song"); s.setUrl(url); return s; }
            @Override
            public boolean downloadRawAudioStreaming(String url, String stagingDir, java.util.function.Consumer<java.io.File> onFileCompleted) {
                if (onFileCompleted != null) {
                    onFileCompleted.accept(mockRaw);
                }
                return true;
            }
            @Override
            public void close() {}
        };

        AudioConversionService mockAudioService = new AudioConversionService() {
            @Override
            public CompletableFuture<java.io.File> convertToMp3(java.io.File rawAudioFile, java.io.File targetMp3File, java.util.function.Consumer<String> statusCallback) {
                try {
                    java.nio.file.Files.writeString(targetMp3File.toPath(), "mp3-content");
                } catch (java.io.IOException ignored) {}
                return CompletableFuture.completedFuture(targetMp3File);
            }
            @Override
            public void close() {}
        };

        AtomicBoolean completed = new AtomicBoolean(false);
        eventBus.subscribe(DownloadEvent.DownloadCompleted.class, e -> completed.set(true));

        try (DownloadCoordinator coordinator = new DownloadCoordinator(mockYtService, queueManager, eventBus, mockAudioService)) {
            coordinator.addToQueue("https://www.youtube.com/watch?v=testpipe");
            coordinator.startDownload();

            Thread.sleep(500);
            assertTrue(completed.get(), "El pipeline debe completar la descarga y conversión");
            assertEquals(1, queueManager.getProcessedCount());
        }
    }

    @Test
    void testPipelineModeConversionFailure(@org.junit.jupiter.api.io.TempDir java.io.File tempDir) throws Exception {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();

        java.io.File mockRaw = new java.io.File(tempDir, "mock_fail.webm");
        java.nio.file.Files.writeString(mockRaw.toPath(), "raw-stream");

        YouTubeDownloadService mockYtService = new YouTubeDownloadService() {
            @Override
            public boolean canHandle(String url) { return true; }
            @Override
            public Song getSongInfo(String url) { Song s = new Song(); s.setTitle("Fail Song"); s.setUrl(url); return s; }
            @Override
            public boolean downloadRawAudioStreaming(String url, String stagingDir, java.util.function.Consumer<java.io.File> onFileCompleted) {
                if (onFileCompleted != null) {
                    onFileCompleted.accept(mockRaw);
                }
                return true;
            }
            @Override
            public void close() {}
        };

        AudioConversionService failingAudioService = new AudioConversionService() {
            @Override
            public CompletableFuture<java.io.File> convertToMp3(java.io.File rawAudioFile, java.io.File targetMp3File, java.util.function.Consumer<String> statusCallback) {
                return CompletableFuture.failedFuture(new RuntimeException("FFmpeg transcode error"));
            }
            @Override
            public void close() {}
        };

        AtomicBoolean failed = new AtomicBoolean(false);
        eventBus.subscribe(DownloadEvent.DownloadFailed.class, e -> failed.set(true));

        try (DownloadCoordinator coordinator = new DownloadCoordinator(mockYtService, queueManager, eventBus, failingAudioService)) {
            coordinator.addToQueue("https://www.youtube.com/watch?v=testfail");
            coordinator.startDownload();

            Thread.sleep(500);
            assertTrue(failed.get(), "El pipeline debe notificar fallo cuando la conversión falla");
            assertEquals(1, queueManager.getFailedCount());
        }
    }

    @Test
    void testResolveTrackTitleFromRawFileName() {
        DownloadService downloadService = new YouTubeDownloadService();
        QueueManager queueManager = new QueueManager();
        EventBus eventBus = new EventBus();

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus)) {
            Song fallbackSong = new Song();
            fallbackSong.setTitle("Main Playlist Card Title");

            // Standard format: raw_ID___Title.ext
            java.io.File file1 = new java.io.File("raw_BuHuChdwJhg___Joaquin Guiller - No Sufriré Por Nadie.mp4");
            assertEquals("Joaquin Guiller - No Sufriré Por Nadie", coordinator.resolveTrackTitle(file1, fallbackSong));

            java.io.File file2 = new java.io.File("raw_yYFDRukciSY___La Aventura - Jhon Alex Castaño.webm");
            assertEquals("La Aventura - Jhon Alex Castaño", coordinator.resolveTrackTitle(file2, fallbackSong));

            // Legacy format with no separator: raw_ID.ext -> falls back to Song title
            java.io.File fileLegacy = new java.io.File("raw_Fs_BnnaEUts.m4a");
            assertEquals("Main Playlist Card Title", coordinator.resolveTrackTitle(fileLegacy, fallbackSong));

            // Legacy format with no separator and null fallback -> uses ID
            java.io.File fileLegacyNoSong = new java.io.File("raw_Fs_BnnaEUts.m4a");
            assertEquals("Fs_BnnaEUts", coordinator.resolveTrackTitle(fileLegacyNoSong, null));
        }
    }

    @Test
    void testSanitizeFilenameHandling() {
        DownloadService downloadService = new YouTubeDownloadService();
        QueueManager queueManager = new QueueManager();
        EventBus eventBus = new EventBus();

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus)) {
            assertEquals("Song_ Title _Remix_", coordinator.sanitizeFilename("Song: Title /Remix?"));
            assertEquals("Clean Name", coordinator.sanitizeFilename("Clean Name...   "));
            assertNotNull(coordinator.sanitizeFilename(null));
        }
    }

    @Test
    void testPipelineMultipleTracksInPlaylistHaveDistinctTitles(@org.junit.jupiter.api.io.TempDir java.io.File tempDir) throws Exception {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();

        java.io.File raw1 = new java.io.File(tempDir, "raw_BuHuChdwJhg___Song One.webm");
        java.io.File raw2 = new java.io.File(tempDir, "raw_yYFDRukciSY___Song Two.webm");
        java.nio.file.Files.writeString(raw1.toPath(), "raw1");
        java.nio.file.Files.writeString(raw2.toPath(), "raw2");

        YouTubeDownloadService mockYtService = new YouTubeDownloadService() {
            @Override
            public boolean canHandle(String url) { return true; }
            @Override
            public Song getSongInfo(String url) {
                Song s = new Song();
                s.setTitle("Playlist First Song");
                s.setUrl(url);
                return s;
            }
            @Override
            public boolean downloadRawAudioStreaming(String url, String stagingDir, java.util.function.Consumer<java.io.File> onFileCompleted) {
                if (onFileCompleted != null) {
                    onFileCompleted.accept(raw1);
                    onFileCompleted.accept(raw2);
                }
                return true;
            }
            @Override
            public void close() {}
        };

        java.util.List<String> convertedFiles = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.List<String> completedTitles = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        AudioConversionService mockAudioService = new AudioConversionService() {
            @Override
            public CompletableFuture<java.io.File> convertToMp3(java.io.File rawAudioFile, java.io.File targetMp3File, java.util.function.Consumer<String> statusCallback) {
                convertedFiles.add(targetMp3File.getName());
                try {
                    java.nio.file.Files.writeString(targetMp3File.toPath(), "mp3");
                } catch (java.io.IOException ignored) {}
                return CompletableFuture.completedFuture(targetMp3File);
            }
            @Override
            public void close() {}
        };

        eventBus.subscribe(DownloadEvent.DownloadCompleted.class, e -> {
            if (e.getSong() != null) {
                completedTitles.add(e.getSong().getTitle());
            }
        });

        try (DownloadCoordinator coordinator = new DownloadCoordinator(mockYtService, queueManager, eventBus, mockAudioService)) {
            coordinator.addToQueue("https://www.youtube.com/playlist?list=PLtest");
            coordinator.startDownload();

            Thread.sleep(600);

            assertTrue(convertedFiles.contains("Song One.mp3"), "Debe haber convertido Song One.mp3");
            assertTrue(convertedFiles.contains("Song Two.mp3"), "Debe haber convertido Song Two.mp3");
            assertNotEquals(convertedFiles.get(0), convertedFiles.get(1), "Los dos archivos MP3 convertidos deben tener nombres distintos");

            assertTrue(completedTitles.contains("Song One"));
            assertTrue(completedTitles.contains("Song Two"));
        }
    }

    @Test
    void shouldNotFinishWhileDownloadCanStillProduceConversions(@org.junit.jupiter.api.io.TempDir java.io.File tempDir) throws Exception {
        EventBus eventBus = new EventBus();
        QueueManager queueManager = new QueueManager();
        CountDownLatch fileDispatched = new CountDownLatch(1);
        CountDownLatch allowDownloadToFinish = new CountDownLatch(1);
        CountDownLatch operationFinished = new CountDownLatch(1);
        AtomicInteger finishEvents = new AtomicInteger();

        java.io.File rawFile = new java.io.File(tempDir, "raw_test___Song.webm");
        java.nio.file.Files.writeString(rawFile.toPath(), "raw");

        YouTubeDownloadService downloadService = new YouTubeDownloadService() {
            @Override
            public boolean canHandle(String url) { return true; }
            @Override
            public Song getSongInfo(String url) { Song song = new Song("Song"); song.setUrl(url); return song; }
            @Override
            public boolean downloadRawAudioStreaming(String url, String stagingDir, java.util.function.Consumer<java.io.File> onFileCompleted) {
                onFileCompleted.accept(rawFile);
                fileDispatched.countDown();
                try {
                    allowDownloadToFinish.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return true;
            }
            @Override
            public void close() {}
        };

        AudioConversionService conversionService = new AudioConversionService() {
            @Override
            public CompletableFuture<java.io.File> convertToMp3(java.io.File rawAudioFile, java.io.File targetMp3File, java.util.function.Consumer<String> statusCallback) {
                return CompletableFuture.completedFuture(targetMp3File);
            }
            @Override
            public void close() {}
        };

        eventBus.subscribe(DownloadEvent.StateChanged.class, event -> {
            if (!event.isDownloading()) {
                finishEvents.incrementAndGet();
                operationFinished.countDown();
            }
        });

        try (DownloadCoordinator coordinator = new DownloadCoordinator(downloadService, queueManager, eventBus, conversionService)) {
            coordinator.addToQueue("https://www.youtube.com/playlist?list=test");
            coordinator.startDownload();

            assertTrue(fileDispatched.await(2, TimeUnit.SECONDS));
            try {
                assertTrue(coordinator.isDownloading());
                assertFalse(coordinator.isQueueEmpty());
                assertEquals(0, finishEvents.get());
            } finally {
                allowDownloadToFinish.countDown();
            }

            assertTrue(operationFinished.await(2, TimeUnit.SECONDS));
            assertFalse(coordinator.isDownloading());
            assertTrue(coordinator.isQueueEmpty());
            assertEquals(1, finishEvents.get());
        }
    }
}
