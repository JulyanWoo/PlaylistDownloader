package com.example.interfaz.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.service.download.DownloadProgressParser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

@SuppressWarnings({"unused", "FXML"})
public class ProgressController implements DownloadProgressParser.ProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressController.class);

    @FXML
    private VBox progressSection;
    @FXML
    private Label currentSongLabel;
    @FXML
    private Label overallProgressLabel;
    @FXML
    private Label overallPercentageLabel;
    @FXML
    private ProgressBar overallProgressBar;
    @FXML
    private Label currentProgressLabel;
    @FXML
    private Label currentPercentageLabel;
    @FXML
    private ProgressBar currentProgressBar;
    @FXML
    private Label downloadSpeedLabel;
    @FXML
    private Button pauseButton;
    @FXML
    private Button resumeButton;
    @FXML
    private Button cancelButton;

    private int totalItems = 0;
    private int currentItem = 0;
    private int downloadedCount = 0;

    private Runnable pauseAction;
    private Runnable resumeAction;
    private Runnable cancelAction;

    public void setPauseAction(Runnable pauseAction) {
        this.pauseAction = pauseAction;
    }

    public void setResumeAction(Runnable resumeAction) {
        this.resumeAction = resumeAction;
    }

    public void setCancelAction(Runnable cancelAction) {
        this.cancelAction = cancelAction;
    }

    @FXML
    private void handlePauseDownload() {
        if (pauseAction != null) {
            pauseAction.run();
        }
        togglePauseResumeButtons(true);
    }

    @FXML
    private void handleResumeDownload() {
        if (resumeAction != null) {
            resumeAction.run();
        }
        togglePauseResumeButtons(false);
    }

    @FXML
    private void handleCancelDownload() {
        if (cancelAction != null) {
            cancelAction.run();
        }
    }

    public void togglePauseResumeButtons(boolean isPaused) {
        Platform.runLater(() -> {
            if (pauseButton != null) {
                pauseButton.setVisible(!isPaused);
                pauseButton.setManaged(!isPaused);
            }
            if (resumeButton != null) {
                resumeButton.setVisible(isPaused);
                resumeButton.setManaged(isPaused);
            }
        });
    }

    public void setComponents(VBox progressSection, Label currentSongLabel,
            Label overallProgressLabel, Label overallPercentageLabel, ProgressBar overallProgressBar,
            Label currentProgressLabel, Label currentPercentageLabel, ProgressBar currentProgressBar) {
        this.progressSection = progressSection;
        this.currentSongLabel = currentSongLabel;
        this.overallProgressLabel = overallProgressLabel;
        this.overallPercentageLabel = overallPercentageLabel;
        this.overallProgressBar = overallProgressBar;
        this.currentProgressLabel = currentProgressLabel;
        this.currentPercentageLabel = currentPercentageLabel;
        this.currentProgressBar = currentProgressBar;
    }

    public void initialize(MainController mainController) {
        resetProgress();
        hideProgressSection();
        LOGGER.info("ProgressController inicializado correctamente con FXML nativo");
    }

    public VBox getProgressSection() {
        return progressSection;
    }

    public ProgressBar getCurrentProgressBar() {
        return currentProgressBar;
    }

    public ProgressBar getOverallProgressBar() {
        return overallProgressBar;
    }

    public Label getCurrentProgressLabel() {
        return currentProgressLabel;
    }

    public Label getOverallProgressLabel() {
        return overallProgressLabel;
    }

    public Label getCurrentPercentageLabel() {
        return currentPercentageLabel;
    }

    public Label getOverallPercentageLabel() {
        return overallPercentageLabel;
    }

    public void showProgressSection() {
        Platform.runLater(() -> {
            if (progressSection != null) {
                progressSection.setVisible(true);
                progressSection.setManaged(true);
            }
        });
    }

    public void hideProgressSection() {
        Platform.runLater(() -> {
            if (progressSection != null) {
                progressSection.setVisible(false);
                progressSection.setManaged(false);
            }
        });
    }

    @Override
    public void onCurrentProgress(double progress, String details) {
        updateCurrentProgress(progress, details);
    }

    public void updateCurrentProgress(double progress, String details) {
        Platform.runLater(() -> {
            if (currentProgressBar != null) {
                currentProgressBar.setProgress(progress);
            }
            if (currentPercentageLabel != null) {
                currentPercentageLabel.setText(String.format("%.1f%%", progress * 100));
            }
            if (currentProgressLabel != null) {
                currentProgressLabel.setText(details);
            }
        });
    }

    @Override
    public void onOverallProgress(int currentItem, int totalItems) {
        updateOverallProgress(currentItem, totalItems);
    }

    public void updateOverallProgress(int currentItem, int totalItems) {
        Platform.runLater(() -> {
            this.currentItem = currentItem;
            this.totalItems = totalItems;
            if (overallProgressLabel != null) {
                overallProgressLabel.setText("📋 Progreso de la Playlist:");
            }
            if (totalItems > 0 && overallProgressBar != null) {
                double progress = (double) currentItem / totalItems;
                overallProgressBar.setProgress(progress);
                if (overallPercentageLabel != null) {
                    overallPercentageLabel.setText(String.format("%d/%d", currentItem, totalItems));
                }
            }
        });
    }

    @Override
    public void onSongStart(String songInfo) {
        updateCurrentSong(songInfo);
    }

    public void updateCurrentSong(String songInfo) {
        Platform.runLater(() -> {
            if (currentSongLabel != null) {
                currentSongLabel.setText("Descargando: " + songInfo);
            }
        });
    }

    @Override
    public void onSpeedUpdate(String speed) {
        updateDownloadSpeed(speed);
    }

    public void updateDownloadSpeed(String speed) {
        Platform.runLater(() -> {
            if (downloadSpeedLabel != null) {
                downloadSpeedLabel.setText(speed);
            }
        });
    }

    @Override
    public void onEtaUpdate(String eta) {
        updateETA(eta);
    }

    public void updateETA(String eta) {
        LOGGER.debug("ETA actualizado: {}", eta);
    }

    @Override
    public void onStatusUpdate(String statusMessage) {
        updateStatus(statusMessage);
    }

    public void updateStatus(String status) {
        LOGGER.debug("Estado actualizado: {}", status);
    }

    @Override
    public void onGenericMessage(String message) {
        handleProgressUpdate(message);
    }

    public void resetProgress() {
        Platform.runLater(() -> {
            if (currentProgressBar != null) {
                currentProgressBar.setProgress(0);
            }
            if (overallProgressBar != null) {
                overallProgressBar.setProgress(0);
            }
            if (currentPercentageLabel != null) {
                currentPercentageLabel.setText("0.0%");
            }
            if (overallPercentageLabel != null) {
                overallPercentageLabel.setText("0/0");
            }
            if (currentSongLabel != null) {
                currentSongLabel.setText("Esperando descarga...");
            }
            totalItems = 0;
            currentItem = 0;
            downloadedCount = 0;
        });
    }

    public void markDownloadCompleted() {
        Platform.runLater(() -> {
            updateStatus("✅ Descarga completada");
            if (currentProgressLabel != null) {
                currentProgressLabel.setText("Todas las descargas completadas");
            }
            if (overallProgressBar != null) {
                overallProgressBar.setProgress(1.0);
            }
            if (overallPercentageLabel != null) {
                overallPercentageLabel.setText("100.0%");
            }
        });
    }

    public void markDownloadPaused() {
        Platform.runLater(() -> updateStatus("Descarga pausada"));
    }

    public void markDownloadCancelled() {
        Platform.runLater(() -> {
            updateStatus("Descarga cancelada");
            hideProgressSection();
        });
    }

    public void handleProgressUpdate(String message) {
        updateStatus(message);
    }
}
