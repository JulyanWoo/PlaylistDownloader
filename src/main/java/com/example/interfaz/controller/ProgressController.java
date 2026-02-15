package com.example.interfaz.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador especializado para la gestión del progreso de descargas
 * Maneja todas las barras de progreso, porcentajes, velocidad y ETA
 */
public class ProgressController {
    
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
    private int totalItems = 0;
    private int currentItem = 0;
    private int downloadedCount = 0;
    
    /**
     * Inyecta los componentes FXML manualmente
     */
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
    
    /**
     * Inicializa el controlador de progreso
     */
    public void initialize(MainController mainController) {
        resetProgress();
        hideProgressSection();
        
        LOGGER.info("ProgressController inicializado correctamente");
    }
    
    public ProgressBar getCurrentProgressBar() { return currentProgressBar; }
    public ProgressBar getOverallProgressBar() { return overallProgressBar; }
    public Label getCurrentProgressLabel() { return currentProgressLabel; }
    public Label getOverallProgressLabel() { return overallProgressLabel; }
    public Label getCurrentPercentageLabel() { return currentPercentageLabel; }
    public Label getOverallPercentageLabel() { return overallPercentageLabel; }
    
    /**
     * Muestra la sección de progreso
     */
    public void showProgressSection() {
        Platform.runLater(() -> {
            progressSection.setVisible(true);
            progressSection.setManaged(true);
            LOGGER.debug("Sección de progreso mostrada");
        });
    }
    
    /**
     * Oculta la sección de progreso
     */
    public void hideProgressSection() {
        Platform.runLater(() -> {
            progressSection.setVisible(false);
            progressSection.setManaged(false);
            LOGGER.debug("Sección de progreso ocultada");
        });
    }
    
    /**
     * Actualiza el progreso actual de una descarga individual
     */
    public void updateCurrentProgress(double progress, String details) {
        Platform.runLater(() -> {
            currentProgressBar.setProgress(progress);
            currentPercentageLabel.setText(String.format("%.1f%%", progress * 100));
            currentProgressLabel.setText(details);
            
            LOGGER.debug("Progreso actual actualizado: {:.1f}% - {}", progress * 100, details);
        });
    }
    
    /**
     * Actualiza el progreso actual usando un mensaje de texto
     */
    public void updateCurrentProgress(String message, double progress) {
        Platform.runLater(() -> {
            currentProgressLabel.setText(message);
            
            if (progress >= 0 && progress <= 1) {
                currentProgressBar.setProgress(progress);
                currentPercentageLabel.setText(String.format("%.1f%%", progress * 100));
            }
            
            LOGGER.debug("Progreso actual actualizado con mensaje: {}", message);
        });
    }
    
    /**
     * Actualiza el progreso general de la playlist/cola
     */
    public void updateOverallProgress(int currentItem, int totalItems) {
        Platform.runLater(() -> {
            this.currentItem = currentItem;
            this.totalItems = totalItems;
            
            overallProgressLabel.setText("📋 Progreso de la Playlist:");
            
            if (totalItems > 0) {
                double progress = (double) currentItem / totalItems;
                overallProgressBar.setProgress(progress);
                overallPercentageLabel.setText(String.format("%d/%d", currentItem, totalItems));
            }
            
            LOGGER.debug("Progreso de la Playlist actualizado: {}/{}", currentItem, totalItems);
        });
    }
    
    /**
     * Actualiza la información de la canción actual
     */
    public void updateCurrentSong(String songInfo) {
        Platform.runLater(() -> {
            currentSongLabel.setText("Descargando: " + songInfo);
            LOGGER.debug("Canción actual actualizada: {}", songInfo);
        });
    }
    
    /**
     * Actualiza la información de la canción actual con índices
     */
    public void updateCurrentSong(int currentIndex, int total, String songName) {
        Platform.runLater(() -> {
            String info = String.format("%d/%d - %s", currentIndex, total, songName);
            currentSongLabel.setText("Descargando: " + info);
            LOGGER.debug("Canción actual actualizada: {}", info);
        });
    }
    
    /**
     * Actualiza la velocidad de descarga y ETA
     */
    public void updateSpeedAndETA(String speed, String eta) {
        Platform.runLater(() -> {
            LOGGER.debug("Velocidad y ETA actualizados: {} - {}", speed, eta);
        });
    }
    
    /**
     * Actualiza solo la velocidad de descarga
     */
    public void updateDownloadSpeed(String speed) {
        Platform.runLater(() -> {
            LOGGER.debug("Velocidad actualizada: {}", speed);
        });
    }
    
    /**
     * Actualiza solo el ETA
     */
    public void updateETA(String eta) {
        Platform.runLater(() -> {
            LOGGER.debug("ETA actualizado: {}", eta);
        });
    }
    
    /**
     * Actualiza el estado general
     */
    public void updateStatus(String status) {
        Platform.runLater(() -> {
            if (!status.contains("[download]") && 
                !status.contains("API JSON") && 
                !status.contains("player API") &&
                !status.startsWith("Iniciando descarga") &&
                !status.contains("Downloading item")) {
                LOGGER.debug("Estado actualizado: {}", status);
            }
        });
    }
    
    /**
     * Actualiza el contador de descargas completadas
     */
    public void updateDownloadedCount(int count) {
        Platform.runLater(() -> {
            this.downloadedCount = count;
            LOGGER.debug("Contador de descargas actualizado: {}", count);
        });
    }
    
    /**
     * Incrementa el contador de descargas completadas
     */
    public void incrementDownloadedCount() {
        updateDownloadedCount(downloadedCount + 1);
    }
    
    /**
     * Resetea todo el progreso a valores iniciales
     */
    public void resetProgress() {
        Platform.runLater(() -> {
            // Resetear barras de progreso
            if (currentProgressBar != null) {
                currentProgressBar.setProgress(0);
                currentProgressBar.setVisible(true);
            }
            if (overallProgressBar != null) {
                overallProgressBar.setProgress(0);
                overallProgressBar.setVisible(true);
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
            if (overallProgressLabel != null) {
                overallProgressLabel.setText("📋 Progreso de la Playlist:");
            }
            totalItems = 0;
            currentItem = 0;
            downloadedCount = 0;
            
            LOGGER.debug("Progreso reseteado con estado inicial limpio");
        });
    }
    
    /**
     * Configura el progreso para el inicio de una nueva descarga
     */
    public void startNewDownload(int totalItems) {
        Platform.runLater(() -> {
            this.totalItems = totalItems;
            this.currentItem = 0;
            this.downloadedCount = 0;
            
            overallProgressLabel.setText("📋 Progreso de la Playlist:");
            overallProgressBar.setProgress(0);
            overallPercentageLabel.setText(String.format("%d/%d", 0, totalItems));
            
            currentProgressBar.setProgress(0);
            currentPercentageLabel.setText("0.0%");
            
            
            showProgressSection();
            
            LOGGER.info("Nueva descarga iniciada con {} elementos", totalItems);
        });
    }
    
    /**
     * Marca la descarga como completada
     */
    public void markDownloadCompleted() {
        Platform.runLater(() -> {
            updateStatus("✅ Descarga completada");
            currentProgressLabel.setText("Todas las descargas completadas");
            
            overallProgressBar.setProgress(1.0);
            overallPercentageLabel.setText("100.0%");
            
            LOGGER.info("Descarga marcada como completada");
        });
    }
    
    /**
     * Marca la descarga como pausada
     */
    public void markDownloadPaused() {
        Platform.runLater(() -> {
            updateStatus("⏸ Descarga pausada");
            LOGGER.info("Descarga marcada como pausada");
        });
    }
    
    /**
     * Marca la descarga como cancelada
     */
    public void markDownloadCancelled() {
        Platform.runLater(() -> {
            updateStatus("Descarga cancelada");
            hideProgressSection();
            LOGGER.info("Descarga marcada como cancelada");
        });
    }
    
    // Getters para acceso al estado
    
    public int getTotalItems() {
        return totalItems;
    }
    
    public int getCurrentItem() {
        return currentItem;
    }
    
    public int getDownloadedCount() {
        return downloadedCount;
    }
    
    public boolean isProgressSectionVisible() {
        return progressSection.isVisible();
    }
    
    // Métodos de compatibilidad con MainController
    
    public void handleProgressUpdate(String message) {
        updateStatus(message);
    }
    
    public void updateDownloadInfo(String speed, String eta) {
        updateSpeedAndETA(speed, eta);
    }
    
    public void updatePlaylistProgress(int currentItem, int totalItems) {
        updateOverallProgress(currentItem, totalItems);
    }
}