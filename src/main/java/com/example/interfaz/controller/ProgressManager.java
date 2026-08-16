package com.example.interfaz.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.factory.DownloadCardFactory;
import com.example.interfaz.factory.ServiceFactory;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressManager.class);

    private VBox progressSection;
    private Button pauseButton;
    private Button resumeButton;
    private VBox activeDownloadsContainer;
    private VBox waitingDownloadsContainer;
    private VBox completedDownloadsContainer;

    private Label activeTitleLabel;
    private Label activeSubtitleLabel;
    private ProgressBar activeProgressBar;
    private Label activeSpeedLabel;

    private String currentSpeed = "0.0 MB/s";
    private String currentEta = "--:--";
    private String currentSongTitle = "Descarga en proceso";
    private double currentProgressValue = 0.0;

    private Runnable pauseAction;
    private Runnable resumeAction;
    private Runnable cancelAction;

    public ProgressManager() {
    }

    public void bindViews(
            VBox progressSection,
            Button pauseButton,
            Button resumeButton,
            VBox activeDownloadsContainer,
            VBox waitingDownloadsContainer,
            VBox completedDownloadsContainer
    ) {
        this.progressSection = progressSection;
        this.pauseButton = pauseButton;
        this.resumeButton = resumeButton;
        this.activeDownloadsContainer = activeDownloadsContainer;
        this.waitingDownloadsContainer = waitingDownloadsContainer;
        this.completedDownloadsContainer = completedDownloadsContainer;
        resetProgress();
    }

    public void setActions(Runnable pauseAction, Runnable resumeAction, Runnable cancelAction) {
        if (pauseAction != null) this.pauseAction = pauseAction;
        if (resumeAction != null) this.resumeAction = resumeAction;
        if (cancelAction != null) this.cancelAction = cancelAction;
    }

    public void setPauseAction(Runnable pauseAction) {
        this.pauseAction = pauseAction;
    }

    public void setResumeAction(Runnable resumeAction) {
        this.resumeAction = resumeAction;
    }

    public void setCancelAction(Runnable cancelAction) {
        this.cancelAction = cancelAction;
    }

    public void handlePauseDownload() {
        if (pauseAction != null) {
            pauseAction.run();
        }
        togglePauseResumeButtons(true);
    }

    public void handleResumeDownload() {
        if (resumeAction != null) {
            resumeAction.run();
        }
        togglePauseResumeButtons(false);
    }

    public void handleCancelDownload() {
        if (cancelAction != null) {
            cancelAction.run();
        }
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

    public void updateCurrentProgress(double progress, String details) {
        this.currentProgressValue = progress;
        Platform.runLater(() -> {
            ensureActiveCardCreated();
            if (activeProgressBar != null) {
                activeProgressBar.setProgress(progress);
            }
            if (activeSpeedLabel != null) {
                activeSpeedLabel.setText(String.format("%.0f%%", progress * 100));
            }
        });
    }

    public void updateOverallProgress(int currentItem, int totalItems) {
        Platform.runLater(() -> {
            ensureActiveCardCreated();
            if (activeSubtitleLabel != null) {
                if (totalItems > 1) {
                    activeSubtitleLabel.setText(currentItem + " de " + totalItems + " canciones");
                } else {
                    activeSubtitleLabel.setText("1 canción");
                }
            }
        });
    }

    public void updateCurrentSong(String songInfo, String urlOrTitle) {
        this.currentSongTitle = songInfo;
        Platform.runLater(() -> {
            ensureActiveCardCreated(songInfo, urlOrTitle);
            if (activeTitleLabel != null) {
                activeTitleLabel.setText(songInfo);
            }
        });
    }

    public void updateDownloadSpeed(String speed) {
        this.currentSpeed = speed;
        Platform.runLater(() -> {
            if (activeSpeedLabel != null) {
                activeSpeedLabel.setText(String.format("%.0f%%", currentProgressValue * 100));
            }
        });
    }

    public void updateETA(String eta) {
        this.currentEta = eta;
    }

    public void updateStatus(String status) {
        LOGGER.debug("Estado: {}", status);
    }

    public void ensureActiveCardCreated() {
        ensureActiveCardCreated(currentSongTitle, currentSongTitle);
    }

    public void ensureActiveCardCreated(String songTitle, String urlOrTitle) {
        if (activeDownloadsContainer == null) {
            return;
        }
        if (activeDownloadsContainer.getChildren().isEmpty()) {
            boolean isPrioritized = false;
            if (waitingDownloadsContainer != null && !waitingDownloadsContainer.getChildren().isEmpty()) {
                Node matchCard = null;
                for (Node child : waitingDownloadsContainer.getChildren()) {
                    Object userData = child.getUserData();
                    if (userData != null && (userData.equals(urlOrTitle) || userData.equals(songTitle))) {
                        matchCard = child;
                        break;
                    }
                }
                if (matchCard != null) {
                    if (Boolean.TRUE.equals(matchCard.getProperties().get("prioritized"))) {
                        isPrioritized = true;
                    }
                    waitingDownloadsContainer.getChildren().remove(matchCard);
                } else {
                    Node removed = waitingDownloadsContainer.getChildren().remove(0);
                    if (Boolean.TRUE.equals(removed.getProperties().get("prioritized"))) {
                        isPrioritized = true;
                    }
                }
            }

            if (!isPrioritized && urlOrTitle != null) {
                isPrioritized = ServiceFactory.getInstance().getQueueManager().isPrioritized(urlOrTitle);
            }

            DownloadCardFactory.ActiveCardElements elements = new DownloadCardFactory.ActiveCardElements();
            VBox card = DownloadCardFactory.createDownloadingCard(
                    songTitle != null ? songTitle : currentSongTitle,
                    "Descargando...",
                    0.0,
                    currentSpeed,
                    currentEta,
                    urlOrTitle,
                    isPrioritized,
                    this::handlePauseDownload,
                    this::handleCancelDownload,
                    elements
            );

            this.activeTitleLabel = elements.getTitleLabel();
            this.activeSubtitleLabel = elements.getSubtitleLabel();
            this.activeProgressBar = elements.getProgressBar();
            this.activeSpeedLabel = elements.getSpeedLabel();

            if (isPrioritized) {
                activeDownloadsContainer.getChildren().add(0, card);
            } else {
                activeDownloadsContainer.getChildren().add(card);
            }
        }
    }

    public void addWaitingCard(String title, String subtitle, String urlOrTitle) {
        Platform.runLater(() -> {
            if (waitingDownloadsContainer == null) {
                return;
            }
            VBox card = DownloadCardFactory.createWaitingCard(title, subtitle, urlOrTitle, this::handlePrioritizeItem);
            waitingDownloadsContainer.getChildren().add(card);
        });
    }

    public void syncWaitingQueue(java.util.List<String> urls) {
        Platform.runLater(() -> {
            if (waitingDownloadsContainer == null) {
                return;
            }
            waitingDownloadsContainer.getChildren().clear();
            for (String url : urls) {
                VBox card = DownloadCardFactory.createWaitingCard(url, "En cola", url, this::handlePrioritizeItem);
                waitingDownloadsContainer.getChildren().add(card);
            }
        });
    }

    public void handlePrioritizeItem(VBox card, String urlOrTitle) {
        if (urlOrTitle != null) {
            ServiceFactory.getInstance().getQueueManager().prioritize(urlOrTitle);
        }
        card.getProperties().put("prioritized", true);
        if (waitingDownloadsContainer != null && waitingDownloadsContainer.getChildren().contains(card)) {
            waitingDownloadsContainer.getChildren().remove(card);
            waitingDownloadsContainer.getChildren().add(0, card);
            LOGGER.info("Elemento priorizado al inicio de la cola: {}", urlOrTitle);
        }
    }

    public void addCompletedCard(String title, String urlOrTitle) {
        Platform.runLater(() -> {
            if (completedDownloadsContainer == null) {
                return;
            }
            VBox card = DownloadCardFactory.createCompletedCard(title, urlOrTitle);
            completedDownloadsContainer.getChildren().add(card);
        });
    }

    public void handleClearQueue() {
        Platform.runLater(() -> {
            if (waitingDownloadsContainer != null) {
                waitingDownloadsContainer.getChildren().clear();
            }
            ServiceFactory.getInstance().getQueueManager().clearQueue();
        });
    }

    public void handleClearCompleted() {
        Platform.runLater(() -> {
            if (completedDownloadsContainer != null) {
                completedDownloadsContainer.getChildren().clear();
            }
        });
    }

    public void setActiveTab(Button targetTab, Button[] tabs) {
        for (Button tab : tabs) {
            if (tab != null) {
                tab.getStyleClass().remove("active");
            }
        }
        if (targetTab != null) {
            targetTab.getStyleClass().add("active");
        }
    }

    public void showContainers(boolean active, boolean waiting, boolean completed) {
        if (activeDownloadsContainer != null && activeDownloadsContainer.getParent() != null) {
            Node parent = activeDownloadsContainer.getParent().getParent();
            parent.setVisible(active);
            parent.setManaged(active);
        }
        if (waitingDownloadsContainer != null && waitingDownloadsContainer.getParent() != null) {
            Node parent = waitingDownloadsContainer.getParent().getParent();
            parent.setVisible(waiting);
            parent.setManaged(waiting);
        }
        if (completedDownloadsContainer != null && completedDownloadsContainer.getParent() != null) {
            Node parent = completedDownloadsContainer.getParent().getParent();
            parent.setVisible(completed);
            parent.setManaged(completed);
        }
    }

    public void resetProgress() {
        Platform.runLater(() -> {
            if (activeDownloadsContainer != null) {
                activeDownloadsContainer.getChildren().clear();
            }
            if (waitingDownloadsContainer != null) {
                waitingDownloadsContainer.getChildren().clear();
            }
            if (completedDownloadsContainer != null) {
                completedDownloadsContainer.getChildren().clear();
            }
            activeTitleLabel = null;
            activeSubtitleLabel = null;
            activeProgressBar = null;
            activeSpeedLabel = null;
        });
    }

    public void markDownloadCompleted() {
        Platform.runLater(() -> {
            if (activeTitleLabel != null) {
                addCompletedCard(activeTitleLabel.getText(), activeTitleLabel.getText());
            }
            if (activeDownloadsContainer != null) {
                activeDownloadsContainer.getChildren().clear();
            }
        });
    }

    public void markDownloadPaused() {
        updateStatus("Descarga pausada");
    }

    public void markDownloadCancelled() {
        resetProgress();
    }
}
