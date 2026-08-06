package com.example.interfaz.controller;

import javafx.application.Platform;

@SuppressWarnings({"unused", "FXML"})
public class ProgressManager {

    public ProgressManager() {
        resetProgressInternal();
    }

    private void resetProgressInternal() {
        Platform.runLater(() -> {
        });
    }

    public final void resetProgress() {
        resetProgressInternal();
    }

    public void updateProgress(double progress) {
        Platform.runLater(() -> {
        });
    }

    public void updateStatus(String message) {
        Platform.runLater(() -> {
        });
    }

    public void updateCurrentSong(String songTitle) {
        Platform.runLater(() -> {
        });
    }

    public void setIndeterminateProgress() {
        Platform.runLater(() -> {
        });
    }

    public void updatePausedState() {
        Platform.runLater(() -> {
        });
    }

    public void updateCancelledState() {
        Platform.runLater(() -> {
        });
    }

    public void updateCompletedState() {
        Platform.runLater(() -> {
        });
    }

    public void updateErrorState(String errorMessage) {
        Platform.runLater(() -> {
        });
    }

    public void updateProgressWithDetails(double progress, String currentSong, String status) {
        Platform.runLater(() -> {
            if (currentSong != null && !currentSong.isEmpty()) {
            }
            if (status != null && !status.isEmpty()) {
            }
        });
    }

    public void updateCurrentProgress(String message, double progress) {
        Platform.runLater(() -> {
        });
    }

    public void updateSpeedAndETA(String speed, String eta) {
        Platform.runLater(() -> {
        });
    }
}
