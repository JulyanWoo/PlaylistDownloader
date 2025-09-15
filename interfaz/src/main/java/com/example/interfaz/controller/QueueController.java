package com.example.interfaz.controller;

import com.example.interfaz.download.QueueManager;
import com.example.interfaz.service.FilterService;
import com.example.interfaz.factory.ServiceFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador especializado para la gestión de la cola de descargas
 * Maneja toda la lógica relacionada con agregar, remover y validar elementos en la cola
 */
public class QueueController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueController.class);
    
    @FXML
    private TextField inputField;
    @FXML
    private ListView<String> queueListView;
    @FXML
    private Label queueCountLabel;
    @FXML
    private Button addButton;
    @FXML
    private Button clearQueueButton;
    @FXML
    private Button removeSelectedButton;
    
    private QueueManager queueManager;
    private FilterService filterService;
    
    /**
     * Inyecta los componentes FXML manualmente
     */
    public void setComponents(TextField inputField, ListView<String> queueListView, 
                            Label queueCountLabel, Button addButton, 
                            Button clearQueueButton, Button removeSelectedButton) {
        this.inputField = inputField;
        this.queueListView = queueListView;
        this.queueCountLabel = queueCountLabel;
        this.addButton = addButton;
        this.clearQueueButton = clearQueueButton;
        this.removeSelectedButton = removeSelectedButton;
    }
    
    /**
     * Inicializa el controlador de cola
     */
    public void initialize(MainController mainController) {
        // Obtener servicios del factory
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        this.filterService = serviceFactory.getFilterService();
        this.queueManager = serviceFactory.createQueueManager(queueListView);
        
        setupEventHandlers();
        
        updateQueueCount();
        
        LOGGER.info("QueueController inicializado correctamente");
    }
    
    /**
     * Configura los manejadores de eventos para los componentes de la cola
     */
    private void setupEventHandlers() {
        inputField.setOnAction(e -> handleAddToQueue());
        
        queueListView.getItems().addListener((javafx.collections.ListChangeListener<String>) change -> {
            updateQueueCount();
        });
        
        queueListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            removeSelectedButton.setDisable(newVal == null);
        });
    }
    
    /**
     * Maneja la adición de URLs a la cola
     */
    @FXML
    public void handleAddToQueue() {
        String input = inputField.getText().trim();
        
        if (input.isEmpty()) {
            showValidationError("Campo vacío", "Por favor ingresa una URL válida.");
            return;
        }
        
        if (!isValidUrl(input)) {
            showValidationError("URL inválida", "La URL ingresada no es válida. Debe ser de YouTube, Spotify o SoundCloud.");
            return;
        }
        
        if (queueManager.contains(input)) {
            showValidationError("URL duplicada", "Esta URL ya está en la cola.");
            return;
        }
        
        queueManager.addToQueue(input);
        inputField.clear();
        
        LOGGER.info("URL agregada a la cola: {}", input);
        showSuccessMessage("URL agregada correctamente a la cola");
    }
    
    /**
     * Limpia toda la cola de descargas
     */
    @FXML
    public void handleClearQueue() {
        if (queueManager.isEmpty()) {
            showValidationError("Cola vacía", "No hay elementos en la cola para limpiar.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmar limpieza");
        confirmAlert.setHeaderText("¿Estás seguro?");
        confirmAlert.setContentText("Se eliminarán todos los elementos de la cola.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int removedCount = queueManager.size();
                queueManager.clearQueue();
                LOGGER.info("Cola limpiada: {} elementos removidos", removedCount);
                showSuccessMessage("Cola limpiada correctamente");
            }
        });
    }
    
    /**
     * Remueve el elemento seleccionado de la cola
     */
    @FXML
    public void handleRemoveSelected() {
        String selectedItem = queueListView.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null) {
            showValidationError("Sin selección", "Por favor selecciona un elemento de la cola para remover.");
            return;
        }
        
        queueManager.removeFromQueue(selectedItem);
        LOGGER.info("Elemento removido de la cola: {}", selectedItem);
        showSuccessMessage("Elemento removido de la cola");
    }
    
    /**
     * Valida si una URL es válida para descargar
     */
    private boolean isValidUrl(String url) {
        if (filterService != null) {
            return filterService.isValidUrl(url);
        }
        
        return url.contains("youtube.com") || url.contains("youtu.be") || 
               url.contains("spotify.com") || url.contains("soundcloud.com");
    }
    
    /**
     * Actualiza el contador de elementos en la cola
     */
    public void updateQueueCount() {
        Platform.runLater(() -> {
            int count = queueManager.size();
            queueCountLabel.setText("Elementos en cola: " + count);
            
            clearQueueButton.setDisable(count == 0);
        });
    }
    
    /**
     * Muestra un mensaje de error de validación
     */
    private void showValidationError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Muestra un mensaje de éxito
     */
    private void showSuccessMessage(String message) {
        Platform.runLater(() -> {
            LOGGER.info("Éxito: {}", message);
        });
    }
        
    /**
     * Verifica si la cola está vacía
     */
    public boolean isQueueEmpty() {
        return queueManager.isEmpty();
    }
    
    /**
     * Obtiene el tamaño de la cola
     */
    public int getQueueSize() {
        return queueManager.size();
    }
    
    /**
     * Habilita o deshabilita los controles de la cola
     */
    public void setControlsEnabled(boolean enabled) {
        Platform.runLater(() -> {
            addButton.setDisable(!enabled);
            clearQueueButton.setDisable(!enabled || queueManager.isEmpty());
            removeSelectedButton.setDisable(!enabled || queueListView.getSelectionModel().getSelectedItem() == null);
            inputField.setDisable(!enabled);
        });
    }
    
    /**
     * Obtiene el QueueManager para uso externo
     */
    public QueueManager getQueueManager() {
        return queueManager;
    }
}