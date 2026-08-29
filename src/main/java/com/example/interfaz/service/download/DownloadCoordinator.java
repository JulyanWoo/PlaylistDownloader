package com.example.interfaz.service.download;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.YouTubeDownloadService;
import com.example.interfaz.util.FileUtils;

public class DownloadCoordinator implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadCoordinator.class);

    private final DownloadService downloadService;
    private final QueueManager queueManager;
    private final EventPublisher eventPublisher;
    private final AudioConversionService audioConversionService;
    private final ExecutorService executor;

    private final AtomicReference<Future<?>> currentFuture = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean downloadsFinished = new AtomicBoolean(true);
    private final AtomicInteger activeConversions = new AtomicInteger(0);

    private volatile boolean isPaused = false;
    private final Object pauseLock = new Object();

    private final java.util.function.Consumer<DownloadEvent.DownloadStarted> startedListener = event -> LOGGER
            .info("Descarga iniciada: {}", event != null && event.getSong() != null ? event.getSong().getTitle() : "");
    private final java.util.function.Consumer<DownloadEvent.DownloadCompleted> completedListener = event -> LOGGER.info(
            "Descarga completada: {}", event != null && event.getSong() != null ? event.getSong().getTitle() : "");
    private final java.util.function.Consumer<DownloadEvent.DownloadFailed> failedListener = event -> {
        String errorMsg = (event != null && event.getError() != null) ? event.getError() : "Error desconocido";
        LOGGER.warn("Descarga fallida: {} - {}",
                event != null && event.getSong() != null ? event.getSong().getTitle() : "", errorMsg);
    };

    public DownloadCoordinator(DownloadService downloadService, QueueManager queueManager,
            EventPublisher eventPublisher) {
        this(downloadService, queueManager, eventPublisher, null);
    }

    public DownloadCoordinator(DownloadService downloadService, QueueManager queueManager,
            EventPublisher eventPublisher, AudioConversionService audioConversionService) {
        this.downloadService = downloadService;
        this.queueManager = queueManager;
        this.eventPublisher = eventPublisher;
        this.audioConversionService = audioConversionService;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DownloadCoordinator-Thread");
            t.setDaemon(true);
            return t;
        });

        setupEventListeners();
    }

    private void setupEventListeners() {
        if (eventPublisher != null) {
            eventPublisher.subscribe(DownloadEvent.DownloadStarted.class, startedListener);
            eventPublisher.subscribe(DownloadEvent.DownloadCompleted.class, completedListener);
            eventPublisher.subscribe(DownloadEvent.DownloadFailed.class, failedListener);
        }
    }

    private void removeEventListeners() {
        if (eventPublisher != null) {
            eventPublisher.unsubscribe(DownloadEvent.DownloadStarted.class, startedListener);
            eventPublisher.unsubscribe(DownloadEvent.DownloadCompleted.class, completedListener);
            eventPublisher.unsubscribe(DownloadEvent.DownloadFailed.class, failedListener);
        }
    }

    public boolean addToQueue(String url) {
        if (closed.get()) {
            throw new IllegalStateException("DownloadCoordinator ya fue cerrado.");
        }
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmed = url.trim();
        if (!downloadService.canHandle(trimmed)) {
            return false;
        }
        if (queueManager.contains(trimmed)) {
            return false;
        }
        boolean added = queueManager.addToQueue(trimmed);
        if (added) {
            LOGGER.info("URL agregada a la cola vía DownloadCoordinator: {}", trimmed);
            publishEvent(new DownloadEvent.QueueUpdated());
        }
        return added;
    }

    public CompletableFuture<Integer> addUrlOrPlaylistAsync(String url) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("DownloadCoordinator ya fue cerrado."));
        }
        return CompletableFuture.supplyAsync(() -> addToQueue(url) ? 1 : 0, executor);
    }

    public void clearQueue() {
        if (closed.get()) {
            throw new IllegalStateException("DownloadCoordinator ya fue cerrado.");
        }
        if (!queueManager.isEmpty()) {
            queueManager.clearQueue();
            LOGGER.info("Cola limpiada vía DownloadCoordinator");
            publishEvent(new DownloadEvent.QueueUpdated());
        }
    }

    public boolean removeFromQueue(String item) {
        if (closed.get()) {
            throw new IllegalStateException("DownloadCoordinator ya fue cerrado.");
        }
        if (item != null && queueManager.isQueued(item)) {
            boolean removed = queueManager.removeFromQueue(item);
            if (removed) {
                LOGGER.info("Elemento removido de la cola vía DownloadCoordinator: {}", item);
                publishEvent(new DownloadEvent.QueueUpdated());
            }
            return removed;
        }
        return false;
    }

    public void startDownload() {
        if (closed.get()) {
            throw new IllegalStateException("DownloadCoordinator ya fue cerrado.");
        }
        if (!running.compareAndSet(false, true)) {
            LOGGER.warn("Ya existe una descarga activa en DownloadCoordinator");
            return;
        }

        if (queueManager.isEmpty()) {
            running.set(false);
            publishEvent(new DownloadEvent.QueueEmpty());
            return;
        }

        downloadsFinished.set(false);
        publishEvent(new DownloadEvent.StateChanged(true, false));

        AtomicReference<Future<?>> futureHolder = new AtomicReference<>();
        Future<?> future = executor.submit(() -> {
            try {
                while (!queueManager.isEmpty() && !Thread.currentThread().isInterrupted()) {
                    String url = null;
                    Song song = null;
                    try {
                        synchronized (pauseLock) {
                            while (isPaused) {
                                pauseLock.wait();
                            }
                        }
                        url = queueManager.pollNext();
                        if (url != null) {
                            song = downloadService.getSongInfo(url);
                            publishEvent(new DownloadEvent.DownloadStarted(song));

                            if (downloadService instanceof YouTubeDownloadService ytService && audioConversionService != null) {
                                String musicDir = FileUtils.getMusicDirectory();
                                String stagingDir = Paths.get(musicDir, ".cache_dl").toString();
                                final Song finalSong = song;
                                final String finalUrl = url;
                                final AtomicInteger filesProcessedForUrl = new AtomicInteger(0);

                                boolean success = ytService.downloadRawAudioStreaming(url, stagingDir, rawAudio -> {
                                    if (rawAudio != null && rawAudio.exists()) {
                                        filesProcessedForUrl.incrementAndGet();
                                        activeConversions.incrementAndGet();

                                        String trackTitle = resolveTrackTitle(rawAudio, finalSong);
                                        String safeTitle = sanitizeFilename(trackTitle);
                                        File targetMp3 = new File(musicDir, safeTitle + ".mp3");

                                        audioConversionService.convertToMp3(rawAudio, targetMp3, msg -> LOGGER.info("[AudioConversion] {}", msg))
                                                .whenComplete((resultFile, error) -> {
                                                    try {
                                                        if (error != null || resultFile == null) {
                                                            LOGGER.error("Fallo en la conversión de audio para archivo {}: {}", rawAudio.getName(), error);
                                                            queueManager.markAsFailed(finalUrl);
                                                            publishEvent(new DownloadEvent.DownloadFailed(finalSong, error != null ? error.getMessage() : "Error en conversión"));
                                                        } else {
                                                            queueManager.markAsCompleted(finalUrl);
                                                            Song itemSong = new Song(trackTitle);
                                                            itemSong.setUrl(finalUrl);
                                                            publishEvent(new DownloadEvent.DownloadCompleted(itemSong, resultFile.getAbsolutePath()));
                                                        }
                                                        publishEvent(new DownloadEvent.QueueUpdated());
                                                    } finally {
                                                        activeConversions.decrementAndGet();
                                                        finishIfComplete();
                                                    }
                                                });
                                    }
                                });

                                if (!success && filesProcessedForUrl.get() == 0) {
                                    queueManager.markAsFailed(url);
                                    publishEvent(new DownloadEvent.DownloadFailed(song, "No se pudo descargar el archivo de audio"));
                                    publishEvent(new DownloadEvent.QueueUpdated());
                                }
                            } else {
                                boolean success = downloadService.downloadSong(url, "").get();

                                if (success && !Thread.currentThread().isInterrupted()) {
                                    queueManager.markAsCompleted(url);
                                    publishEvent(new DownloadEvent.DownloadCompleted(song, url));
                                } else {
                                    queueManager.markAsFailed(url);
                                    publishEvent(new DownloadEvent.DownloadFailed(song, "Error procesando canción"));
                                }
                                publishEvent(new DownloadEvent.QueueUpdated());
                            }
                        }
                    } catch (InterruptedException e) {
                        LOGGER.info("Hilo de descarga cancelado por solicitud del usuario");
                        if (url != null) {
                            queueManager.markAsFailed(url);
                        }
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        LOGGER.error("Error procesando descarga de canción: {}", url, e);
                        if (url != null) {
                            queueManager.markAsFailed(url);
                            publishEvent(new DownloadEvent.DownloadFailed(song, e.getMessage()));
                        }
                    }
                }
            } finally {
                currentFuture.compareAndSet(futureHolder.get(), null);
                downloadsFinished.set(true);
                finishIfComplete();
            }
        });
        futureHolder.set(future);
        currentFuture.set(future);

        LOGGER.info("Proceso de descarga en segundo plano iniciado vía ExecutorService desacoplado");
    }

    public String resolveTrackTitle(File rawAudio, Song fallbackSong) {
        if (rawAudio != null) {
            String nameWithoutExt = rawAudio.getName().replaceFirst("[.][^.]+$", "");
            if (nameWithoutExt.startsWith("raw_")) {
                String remainder = nameWithoutExt.substring(4);
                int sepIndex = remainder.indexOf("___");
                if (sepIndex != -1) {
                    String titleFromFilename = remainder.substring(sepIndex + 3).trim();
                    if (!titleFromFilename.isEmpty()) {
                        return titleFromFilename;
                    }
                }
            }
        }
        if (fallbackSong != null && fallbackSong.getTitle() != null && !fallbackSong.getTitle().isBlank()) {
            return fallbackSong.getTitle();
        }
        if (rawAudio != null) {
            return rawAudio.getName().replaceFirst("[.][^.]+$", "").replaceFirst("^raw_", "");
        }
        return "Track";
    }

    public String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "audio_" + System.currentTimeMillis();
        }
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        while (sanitized.endsWith(".") || sanitized.endsWith(" ")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }
        return sanitized.isEmpty() ? "audio_" + System.currentTimeMillis() : sanitized;
    }

    public boolean isDownloading() {
        return running.get() || activeConversions.get() > 0;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public boolean isQueueEmpty() {
        return queueManager.isEmpty() && downloadsFinished.get() && activeConversions.get() == 0 && !running.get();
    }

    private void finishIfComplete() {
        if (downloadsFinished.get() && queueManager.isEmpty() && activeConversions.get() == 0
                && running.compareAndSet(true, false)) {
            publishEvent(new DownloadEvent.StateChanged(false, false));
        }
    }

    public void pauseDownload() {
        if (!isDownloading()) {
            return;
        }
        pause();
        downloadService.pauseDownload();
        publishEvent(new DownloadEvent.StateChanged(true, true));
        LOGGER.info("Descarga pausada vía DownloadCoordinator");
    }

    public void resumeDownload() {
        if (!isDownloading()) {
            return;
        }
        resume();
        downloadService.resumeDownload();
        publishEvent(new DownloadEvent.StateChanged(true, false));
        LOGGER.info("Descarga reanudada vía DownloadCoordinator");
    }

    public void cancelDownload() {
        Future<?> future = currentFuture.getAndSet(null);
        if (future != null) {
            future.cancel(true);
        }
        downloadService.stopDownload();
        if (audioConversionService != null) {
            audioConversionService.cancelAll();
        }
        activeConversions.set(0);
        downloadsFinished.set(true);
        queueManager.clearAll();
        cleanStagingDirectory();
        if (running.getAndSet(false)) {
            publishEvent(new DownloadEvent.StateChanged(false, false));
        }
        publishEvent(new DownloadEvent.QueueUpdated());
        LOGGER.info("Descarga cancelada y estado totalmente reiniciado vía DownloadCoordinator");
    }

    private void cleanStagingDirectory() {
        try {
            Path stagingDir = Paths.get(FileUtils.getMusicDirectory(), ".cache_dl");
            if (Files.exists(stagingDir)) {
                try (var stream = Files.list(stagingDir)) {
                    stream.forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.debug("No se pudo limpiar el directorio de staging: {}", e.getMessage());
        }
    }

    private void publishEvent(DownloadEvent event) {
        if (eventPublisher != null) {
            eventPublisher.publish(event);
        }
    }

    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        cancelDownload();
        removeEventListeners();
        try {
            if (downloadService != null) {
                downloadService.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error al cerrar downloadService: {}", e.getMessage());
        }
        try {
            if (audioConversionService != null) {
                audioConversionService.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error al cerrar audioConversionService: {}", e.getMessage());
        }
        executor.shutdownNow();
        LOGGER.info("DownloadCoordinator destruido y listeners removidos");
    }

    @Override
    public void close() {
        shutdown();
    }
}
