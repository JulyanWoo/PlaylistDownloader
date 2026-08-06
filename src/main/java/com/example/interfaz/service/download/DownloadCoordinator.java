package com.example.interfaz.service.download;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.DownloadService;

public class DownloadCoordinator implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadCoordinator.class);

    private final DownloadService downloadService;
    private final QueueManager queueManager;
    private final EventPublisher eventPublisher;
    private final ExecutorService executor;

    private final AtomicReference<Future<?>> currentFuture = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

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
        this.downloadService = downloadService;
        this.queueManager = queueManager;
        this.eventPublisher = eventPublisher;
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
                if (running.getAndSet(false)) {
                    publishEvent(new DownloadEvent.StateChanged(false, false));
                }
            }
        });
        futureHolder.set(future);
        currentFuture.set(future);

        LOGGER.info("Proceso de descarga en segundo plano iniciado vía ExecutorService desacoplado");
    }

    public boolean isDownloading() {
        return running.get();
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
        return queueManager.isEmpty();
    }

    public void pauseDownload() {
        if (!running.get())
            return;
        pause();
        downloadService.pauseDownload();
        publishEvent(new DownloadEvent.StateChanged(true, true));
        LOGGER.info("Descarga pausada vía DownloadCoordinator");
    }

    public void resumeDownload() {
        if (!running.get())
            return;
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
        if (running.getAndSet(false)) {
            publishEvent(new DownloadEvent.StateChanged(false, false));
        }
        LOGGER.info("Descarga cancelada vía DownloadCoordinator");
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
        executor.shutdownNow();
        LOGGER.info("DownloadCoordinator destruido y listeners removidos");
    }

    @Override
    public void close() {
        shutdown();
    }
}
