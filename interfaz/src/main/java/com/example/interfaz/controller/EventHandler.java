package com.example.interfaz.controller;

import com.example.interfaz.service.DownloadService;
import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.DownloadEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maneja los eventos de la interfaz de usuario
 * Extrae la lógica de eventos del MainController para reducir su tamaño
 */
public class EventHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);
    
    private final DownloadService downloadService;
    private final UIStateManager uiStateManager;
    private final ProgressManager progressManager;
    private final QueueManager queueManager;
    private final EventPublisher eventPublisher;
    
    // Referencias a componentes UI
    private final TextField inputField;
    private final ListView<String> queueListView;
    
    public EventHandler(MainController mainController, DownloadService downloadService, 
                       UIStateManager uiStateManager, 
                       ProgressManager progressManager, QueueManager queueManager, 
                       EventPublisher eventPublisher, TextField inputField, ListView<String> queueListView) {
        this.downloadService = downloadService;
        this.uiStateManager = uiStateManager;
        this.progressManager = progressManager;
        this.queueManager = queueManager;
        this.eventPublisher = eventPublisher;
        this.inputField = inputField;
        this.queueListView = queueListView;
        
        setupEventListeners();
    }
    
    private void setupEventListeners() {
        eventPublisher.subscribe(DownloadEvent.DownloadStarted.class, this::onDownloadStarted);
        eventPublisher.subscribe(DownloadEvent.DownloadCompleted.class, this::onDownloadCompleted);
        eventPublisher.subscribe(DownloadEvent.DownloadFailed.class, this::onDownloadFailed);
    }
    
    private void onDownloadStarted(DownloadEvent.DownloadStarted event) {
        LOGGER.info("Descarga iniciada: {}", event.getSong().getTitle());
    }
    
    private void onDownloadCompleted(DownloadEvent.DownloadCompleted event) {
        LOGGER.info("Descarga completada: {}", event.getSong().getTitle());
        showAlert("Éxito", "Descarga completada: " + event.getSong().getTitle());
    }
    
    private void onDownloadFailed(DownloadEvent.DownloadFailed event) {
        LOGGER.warn("Descarga fallida: {} - {}", event.getSong().getTitle(), event.getError());
        showAlert("Error", "Error en descarga: " + event.getError());
    }
    
    public void handleAddToQueue() {
        String url = inputField.getText().trim();
        if (!url.isEmpty()) {
            if (downloadService.canHandle(url)) {
                queueManager.addToQueue(url);
                inputField.clear();
            } else {
                showAlert("URL no válida", "La URL proporcionada no es compatible.");
            }
        }
    }
    
    public void handleClearQueue() {
        if (!queueManager.isEmpty()) {
            queueManager.clearQueue();
        } else {
            showAlert("Cola vacía", "No hay elementos en la cola para limpiar.");
        }
    }
    
    public void handleRemoveSelected() {
        String selectedItem = queueListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            queueManager.removeFromQueue(selectedItem);
        } else {
            showAlert("Sin selección", "Por favor, selecciona un elemento para eliminar.");
        }
    }
    
    public Task<Void> createDownloadTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    uiStateManager.setDownloadingState(true);
                    
                    while (!queueManager.isEmpty() && !isCancelled()) {
                        String url = queueManager.getNextUrl();
                        if (url != null) {
                            Platform.runLater(() -> progressManager.updateCurrentSong("Descargando: " + url));
                            
                            Task<Void> songTask = downloadService.downloadSong(url, "");
                            Thread songThread = new Thread(songTask);
                            songThread.start();
                            songThread.join();
                            
                            if (!isCancelled()) {
                                queueManager.markAsCompleted(url);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    LOGGER.info("Descarga interrumpida por cancelación del usuario");
                } catch (Exception e) {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Error desconocido";
                    LOGGER.error("Error durante la descarga: {}", errorMessage, e);
                    Platform.runLater(() -> showAlert("Error", "Error durante la descarga: " + errorMessage));
                } finally {
                    Platform.runLater(() -> {
                        uiStateManager.setDownloadingState(false);
                        progressManager.resetProgress();
                    });
                }
                return null;
            }
        };
    }
    
    public void handleStartDownload() {
        LOGGER.info("Iniciando descarga...");
    }
    
    public void handlePauseDownload() {
        LOGGER.info("Pausando descarga...");
    }
    
    public void handleCancelDownload() {
        LOGGER.info("Cancelando descarga...");
    }
    
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}