package com.example.interfaz.service.download;

import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.event.EventBus;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.YouTubeDownloadService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
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
}
