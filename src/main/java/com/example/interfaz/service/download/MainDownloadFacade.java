package com.example.interfaz.service.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.controller.ProgressController;
import com.example.interfaz.controller.QueueController;
import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.YouTubeDownloadService;

import javafx.application.Platform;

public class MainDownloadFacade implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainDownloadFacade.class);

    private final DownloadService downloadService;
    private final DownloadProgressParser progressParser;
    private final YtDlpUpdateService ytDlpUpdateService;
    private final EventPublisher eventPublisher;

    private DownloadCoordinator downloadCoordinator;

    public interface DownloadQueuedHandler { void onQueued(DownloadEvent.DownloadStarted event); }
    public interface DownloadStartedHandler { void onStarted(DownloadEvent.DownloadStarted event); }
    public interface DownloadProgressHandler { void onProgress(DownloadEvent.DownloadProgress event); }
    public interface DownloadCompletedHandler { void onCompleted(DownloadEvent.DownloadCompleted event); }
    public interface DownloadErrorHandler { void onError(DownloadEvent.DownloadFailed event); }

    private DownloadQueuedHandler queuedHandler;
    private DownloadStartedHandler startedHandler;
    private DownloadProgressHandler progressHandler;
    private DownloadCompletedHandler completedHandler;
    private DownloadErrorHandler errorHandler;

    public MainDownloadFacade(
            DownloadService downloadService,
            DownloadProgressParser progressParser,
            YtDlpUpdateService ytDlpUpdateService,
            EventPublisher eventPublisher
    ) {
        this.downloadService = downloadService;
        this.progressParser = progressParser;
        this.ytDlpUpdateService = ytDlpUpdateService;
        this.eventPublisher = eventPublisher;
    }

    public void initialize(
            QueueController queueController,
            ProgressController progressController,
            Runnable onQueueEmptyCallback
    ) {
        QueueManager queueManager = ServiceFactory.getInstance().getQueueManager();
        this.downloadCoordinator = new DownloadCoordinator(
                downloadService,
                queueManager,
                eventPublisher
        );

        if (downloadService instanceof YouTubeDownloadService ytService) {
            ytService.setProgressCallback(progressParser::parseAndDispatch);
        }

        if (progressController != null) {
            progressParser.setListener(progressController);
            progressController.setPauseAction(downloadCoordinator::pauseDownload);
            progressController.setResumeAction(downloadCoordinator::resumeDownload);
            progressController.setCancelAction(() -> {
                downloadCoordinator.cancelDownload();
                progressController.markDownloadCancelled();
                progressController.togglePauseResumeButtons(false);
                if (queueController != null) {
                    queueController.setControlsEnabled(true);
                }
            });
        }

        if (eventPublisher != null) {
            eventPublisher.subscribe(DownloadEvent.StateChanged.class, event -> Platform.runLater(() -> {
                if (queueController != null) {
                    queueController.setControlsEnabled(!event.isDownloading());
                }
                if (progressController != null) {
                    progressController.togglePauseResumeButtons(event.isPaused());
                }
                if (!event.isDownloading() && progressController != null && downloadCoordinator.isQueueEmpty()) {
                    progressController.markDownloadCompleted();
                    progressController.togglePauseResumeButtons(false);
                }
            }));

            eventPublisher.subscribe(DownloadEvent.DownloadStarted.class, event -> Platform.runLater(() -> {
                if (progressController != null) {
                    progressController.showProgressSection();
                    if (event.getSong() != null) {
                        progressController.updateCurrentSong(event.getSong().getTitle(), event.getSong().getUrl());
                    }
                }
            }));
        }

        LOGGER.info("MainDownloadFacade inicializado con progreso y acciones de control.");
    }

    public boolean addToQueue(String url) {
        if (downloadCoordinator != null) {
            return downloadCoordinator.addToQueue(url);
        }
        LOGGER.warn("DownloadCoordinator no inicializado al intentar agregar URL: {}", url);
        return false;
    }

    public void startNextDownload() {
        if (downloadCoordinator != null) {
            downloadCoordinator.startDownload();
        }
    }

    public void cancelCurrentDownload() {
        if (downloadCoordinator != null) {
            downloadCoordinator.cancelDownload();
        }
    }

    public boolean isDownloading() {
        return downloadCoordinator != null && downloadCoordinator.isDownloading();
    }

    public DownloadCoordinator getDownloadCoordinator() {
        return downloadCoordinator;
    }

    public DownloadProgressParser getProgressParser() {
        return progressParser;
    }

    public YtDlpUpdateService getYtDlpUpdateService() {
        return ytDlpUpdateService;
    }

    public void subscribeToEvents(
            DownloadQueuedHandler onQueued,
            DownloadStartedHandler onStarted,
            DownloadProgressHandler onProgress,
            DownloadCompletedHandler onCompleted,
            DownloadErrorHandler onError
    ) {
        this.queuedHandler = onQueued;
        this.startedHandler = onStarted;
        this.progressHandler = onProgress;
        this.completedHandler = onCompleted;
        this.errorHandler = onError;

        if (eventPublisher != null) {
            eventPublisher.subscribe(DownloadEvent.DownloadStarted.class, this::handleQueued);
            eventPublisher.subscribe(DownloadEvent.DownloadStarted.class, this::handleStarted);
            eventPublisher.subscribe(DownloadEvent.DownloadProgress.class, this::handleProgress);
            eventPublisher.subscribe(DownloadEvent.DownloadCompleted.class, this::handleCompleted);
            eventPublisher.subscribe(DownloadEvent.DownloadFailed.class, this::handleError);
        }
    }

    public void unsubscribeFromEvents() {
        if (eventPublisher != null) {
            eventPublisher.unsubscribe(DownloadEvent.DownloadStarted.class, this::handleQueued);
            eventPublisher.unsubscribe(DownloadEvent.DownloadStarted.class, this::handleStarted);
            eventPublisher.unsubscribe(DownloadEvent.DownloadProgress.class, this::handleProgress);
            eventPublisher.unsubscribe(DownloadEvent.DownloadCompleted.class, this::handleCompleted);
            eventPublisher.unsubscribe(DownloadEvent.DownloadFailed.class, this::handleError);
        }
        this.queuedHandler = null;
        this.startedHandler = null;
        this.progressHandler = null;
        this.completedHandler = null;
        this.errorHandler = null;
    }

    private void handleQueued(DownloadEvent.DownloadStarted e) {
        if (queuedHandler != null) queuedHandler.onQueued(e);
    }
    private void handleStarted(DownloadEvent.DownloadStarted e) {
        if (startedHandler != null) startedHandler.onStarted(e);
    }
    private void handleProgress(DownloadEvent.DownloadProgress e) { if (progressHandler != null) progressHandler.onProgress(e); }
    private void handleCompleted(DownloadEvent.DownloadCompleted e) { if (completedHandler != null) completedHandler.onCompleted(e); }
    private void handleError(DownloadEvent.DownloadFailed e) { if (errorHandler != null) errorHandler.onError(e); }

    @Override
    public void close() {
        unsubscribeFromEvents();

        if (downloadCoordinator != null) {
            try {
                downloadCoordinator.close();
                LOGGER.info("DownloadCoordinator liberado correctamente vía MainDownloadFacade.close()");
            } catch (Exception e) {
                LOGGER.error("Error al cerrar DownloadCoordinator", e);
            } finally {
                downloadCoordinator = null;
            }
        }
    }
}
