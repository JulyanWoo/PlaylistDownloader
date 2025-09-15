package com.example.interfaz.controller;

import javafx.scene.control.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestor de estados de la interfaz de usuario
 * Responsable de manejar la habilitación/deshabilitación y visibilidad de componentes
 */
public class UIStateManager {
    
    private TextField inputField;
    private Button addButton;
    private Button startButton;
    private Button pauseButton;
    private Button cancelButton;
    private Button clearQueueButton;
    private Button removeSelectedButton;
    private ListView<String> queueListView;
    
    private final AtomicBoolean isDownloading = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    
    /**
     * Constructor que inicializa el gestor con los componentes de la UI
     */
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
    
    /**
     * Inicializa el estado por defecto de la UI
     */
    private void initializeUI() {
        pauseButton.setVisible(false);
        cancelButton.setVisible(false);
        pauseButton.setText("⏸ Pausar");
    }
    
    /**
     * Actualiza el estado de la interfaz según el estado de descarga
     * @param downloading true si está descargando
     */
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
    
    /**
     * Actualiza el estado de pausa
     * @param paused true si está pausado
     */
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
    
    /**
     * Habilita todos los controles (estado inicial)
     */
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
    
    // Getters para los estados
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
    
    /**
     * Establece el estado de pausa
     */
    public void setPausedState(boolean paused) {
        isPaused.set(paused);
        updatePauseState(paused);
    }
    
    /**
     * Establece el estado de descarga
     */
    public void setDownloadingState(boolean downloading) {
        isDownloading.set(downloading);
        if (!downloading) {
            shouldStop.set(false);
        }
        updateDownloadState(downloading);
    }
    
    /**
     * Establece si se debe detener
     */
    public void setShouldStop(boolean shouldStop) {
        this.shouldStop.set(shouldStop);
    }
    
    /**
     * Verifica si se debe detener
     */
    public boolean shouldStop() {
        return shouldStop.get();
    }
    

    

}