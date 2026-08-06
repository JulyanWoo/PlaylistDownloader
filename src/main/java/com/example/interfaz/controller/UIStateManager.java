package com.example.interfaz.controller;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class UIStateManager {

    private final TextField inputField;
    private final Button addButton;
    private final Button startButton;
    private final Button pauseButton;
    private final Button cancelButton;
    private final Button clearQueueButton;
    private final Button removeSelectedButton;
    private final ListView<String> queueListView;

    private final AtomicBoolean isDownloading = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    public UIStateManager(TextField inputField, Button addButton, Button startButton,
                         Button pauseButton, Button cancelButton, Button clearQueueButton,
                         Button removeSelectedButton, ListView<String> queueListView) {
        this.inputField = inputField;
        this.addButton = addButton;
        this.startButton = startButton;
        this.pauseButton = pauseButton;
        this.cancelButton = cancelButton;
        this.clearQueueButton = clearQueueButton;
        this.removeSelectedButton = removeSelectedButton;
        this.queueListView = queueListView;

        initializeUI();
    }

    private void initializeUI() {
        pauseButton.setVisible(false);
        cancelButton.setVisible(false);
        pauseButton.setText("⏸ Pausar");
    }

    public void updateDownloadState(boolean downloading) {
        isDownloading.set(downloading);

        addButton.setDisable(downloading && !isPaused.get());
        startButton.setDisable(downloading);
        clearQueueButton.setDisable(downloading);
        removeSelectedButton.setDisable(downloading);

        pauseButton.setVisible(downloading);
        pauseButton.setDisable(!downloading);
        cancelButton.setVisible(downloading);
        cancelButton.setDisable(!downloading);

        inputField.setDisable(downloading && !isPaused.get());
        queueListView.setDisable(false);

        if (!downloading) {
            pauseButton.setText("⏸ Pausar");
            isPaused.set(false);
        }
    }

    public void updatePauseState(boolean paused) {
        isPaused.set(paused);

        if (isDownloading.get()) {
            if (paused) {
                pauseButton.setText("▶️ Reanudar");
                inputField.setDisable(false);
                addButton.setDisable(false);
            } else {
                pauseButton.setText("⏸ Pausar");
                inputField.setDisable(true);
                addButton.setDisable(true);
            }
        }
    }

    public void enableAllControls() {
        addButton.setDisable(false);
        startButton.setDisable(false);
        clearQueueButton.setDisable(false);
        removeSelectedButton.setDisable(false);
        inputField.setDisable(false);
        queueListView.setDisable(false);

        pauseButton.setVisible(false);
        cancelButton.setVisible(false);

        isDownloading.set(false);
        isPaused.set(false);
    }

    public boolean isDownloading() {
        return isDownloading.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public AtomicBoolean getIsDownloadingAtomic() {
        return isDownloading;
    }

    public AtomicBoolean getIsPausedAtomic() {
        return isPaused;
    }

    public void setPausedState(boolean paused) {
        isPaused.set(paused);
        updatePauseState(paused);
    }

    public void setDownloadingState(boolean downloading) {
        isDownloading.set(downloading);
        if (!downloading) {
            shouldStop.set(false);
        }
        updateDownloadState(downloading);
    }

    public void setShouldStop(boolean shouldStop) {
        this.shouldStop.set(shouldStop);
    }

    public boolean shouldStop() {
        return shouldStop.get();
    }

}
