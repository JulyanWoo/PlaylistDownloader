package com.example.interfaz.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.service.download.DownloadProgressParser;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

@SuppressWarnings({"unused", "FXML"})
public class ProgressController implements DownloadProgressParser.ProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressController.class);

    @FXML private VBox progressSection;
    @FXML private Button pauseButton;
    @FXML private Button resumeButton;
    @FXML private Button cancelButton;
    @FXML private Button clearQueueButton;
    @FXML private Button clearCompletedButton;

    @FXML private Button filterTabAll;
    @FXML private Button filterTabDownloading;
    @FXML private Button filterTabWaiting;
    @FXML private Button filterTabCompleted;

    @FXML private VBox activeDownloadsContainer;
    @FXML private VBox waitingDownloadsContainer;
    @FXML private VBox completedDownloadsContainer;

    private final ProgressManager progressManager = new ProgressManager();

    public ProgressController() {
    }

    public void setPauseAction(Runnable pauseAction) {
        progressManager.setPauseAction(pauseAction);
    }

    public void setResumeAction(Runnable resumeAction) {
        progressManager.setResumeAction(resumeAction);
    }

    public void setCancelAction(Runnable cancelAction) {
        progressManager.setCancelAction(cancelAction);
    }

    @FXML
    public void initialize() {
        progressManager.bindViews(
                progressSection,
                pauseButton,
                resumeButton,
                activeDownloadsContainer,
                waitingDownloadsContainer,
                completedDownloadsContainer
        );
        LOGGER.info("ProgressController inicializado correctamente con tarjetas interactivas");
    }

    public void initialize(MainController mainController) {
        initialize();
    }

    public VBox getProgressSection() {
        return progressSection;
    }

    public void showProgressSection() {
        progressManager.showProgressSection();
    }

    public void hideProgressSection() {
        progressManager.hideProgressSection();
    }

    @FXML
    private void handlePauseDownload() {
        progressManager.handlePauseDownload();
    }

    @FXML
    private void handleResumeDownload() {
        progressManager.handleResumeDownload();
    }

    @FXML
    private void handleCancelDownload() {
        progressManager.handleCancelDownload();
    }

    @FXML
    private void handleClearQueue() {
        progressManager.handleClearQueue();
    }

    @FXML
    private void handleClearCompleted() {
        progressManager.handleClearCompleted();
    }

    @FXML
    void onFilterTabAll() {
        progressManager.setActiveTab(filterTabAll, getFilterTabs());
        progressManager.showContainers(true, true, true);
    }

    @FXML
    void onFilterTabDownloading() {
        progressManager.setActiveTab(filterTabDownloading, getFilterTabs());
        progressManager.showContainers(true, false, false);
    }

    @FXML
    void onFilterTabWaiting() {
        progressManager.setActiveTab(filterTabWaiting, getFilterTabs());
        progressManager.showContainers(false, true, false);
    }

    @FXML
    void onFilterTabCompleted() {
        progressManager.setActiveTab(filterTabCompleted, getFilterTabs());
        progressManager.showContainers(false, false, true);
    }

    private Button[] getFilterTabs() {
        return new Button[]{filterTabAll, filterTabDownloading, filterTabWaiting, filterTabCompleted};
    }

    public void togglePauseResumeButtons(boolean isPaused) {
        progressManager.togglePauseResumeButtons(isPaused);
    }

    @Override
    public void onCurrentProgress(double progress, String details) {
        updateCurrentProgress(progress, details);
    }

    public void updateCurrentProgress(double progress, String details) {
        progressManager.updateCurrentProgress(progress, details);
    }

    @Override
    public void onOverallProgress(int currentItem, int totalItems) {
        updateOverallProgress(currentItem, totalItems);
    }

    public void updateOverallProgress(int currentItem, int totalItems) {
        progressManager.updateOverallProgress(currentItem, totalItems);
    }

    @Override
    public void onSongStart(String songInfo) {
        updateCurrentSong(songInfo);
    }

    public void updateCurrentSong(String songInfo) {
        updateCurrentSong(songInfo, songInfo);
    }

    public void updateCurrentSong(String songInfo, String urlOrTitle) {
        progressManager.updateCurrentSong(songInfo, urlOrTitle);
    }

    @Override
    public void onSpeedUpdate(String speed) {
        updateDownloadSpeed(speed);
    }

    public void updateDownloadSpeed(String speed) {
        progressManager.updateDownloadSpeed(speed);
    }

    @Override
    public void onEtaUpdate(String eta) {
        updateETA(eta);
    }

    public void updateETA(String eta) {
        progressManager.updateETA(eta);
    }

    @Override
    public void onStatusUpdate(String statusMessage) {
        updateStatus(statusMessage);
    }

    public void updateStatus(String status) {
        progressManager.updateStatus(status);
    }

    @Override
    public void onGenericMessage(String message) {
        handleProgressUpdate(message);
    }

    public void addWaitingCard(String title, String subtitle) {
        addWaitingCard(title, subtitle, title);
    }

    public void addWaitingCard(String title, String subtitle, String urlOrTitle) {
        progressManager.addWaitingCard(title, subtitle, urlOrTitle);
    }

    public void addCompletedCard(String title) {
        addCompletedCard(title, title);
    }

    public void addCompletedCard(String title, String urlOrTitle) {
        progressManager.addCompletedCard(title, urlOrTitle);
    }

    public void resetProgress() {
        progressManager.resetProgress();
    }

    public void markDownloadCompleted() {
        progressManager.markDownloadCompleted();
    }

    public void markDownloadPaused() {
        progressManager.markDownloadPaused();
    }

    public void markDownloadCancelled() {
        progressManager.markDownloadCancelled();
    }

    public void handleProgressUpdate(String message) {
        progressManager.updateStatus(message);
    }
}
