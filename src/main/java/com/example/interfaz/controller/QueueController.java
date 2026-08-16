package com.example.interfaz.controller;

import com.example.interfaz.download.QueueManager;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.FilterService;
import com.example.interfaz.service.ui.DialogService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unused", "FXML"})
public class QueueController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueController.class);

    @FXML private TextField inputField;
    @FXML private ListView<String> queueListView;
    @FXML private Label queueCountLabel;
    @FXML private Button addButton;
    @FXML private Button clearQueueButton;
    @FXML private Button removeSelectedButton;

    private final ObservableList<String> queueItems = FXCollections.observableArrayList();

    private QueueManager queueManager;
    private FilterService filterService;
    private DialogService dialogService;
    private com.example.interfaz.event.EventPublisher eventPublisher;

    @FXML
    void initialize() {
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        this.filterService = serviceFactory.getFilterService();
        this.dialogService = serviceFactory.getDialogService();
        this.queueManager = serviceFactory.getQueueManager();
        this.eventPublisher = serviceFactory.getEventPublisher();

        if (queueListView != null) {
            queueListView.setItems(queueItems);
            queueListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }

        if (this.eventPublisher != null) {
            this.eventPublisher.subscribe(com.example.interfaz.event.DownloadEvent.QueueUpdated.class, event -> refreshQueue());
        }

        setupEventHandlers();
        refreshQueue();

        LOGGER.info("QueueController desacoplado e inyectado correctamente");
    }

    public void refreshQueue() {
        Platform.runLater(() -> {
            if (queueManager != null) {
                queueItems.setAll(queueManager.getAllItems());
                updateQueueCount();
            }
        });
    }

    private void setupEventHandlers() {
        if (inputField != null) {
            inputField.setOnAction(e -> handleAddToQueue());
        }

        if (queueListView != null) {
            queueItems.addListener((javafx.collections.ListChangeListener<String>) change -> {
                updateQueueCount();
            });

            queueListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (removeSelectedButton != null) {
                    removeSelectedButton.setDisable(newVal == null);
                }
            });
        }
    }

    @FXML
    public void handleAddToQueue() {
        if (inputField == null) return;
        String input = inputField.getText().trim();

        if (input.isEmpty()) {
            dialogService.showError("Campo vacío", "Por favor ingresa una URL válida.");
            return;
        }

        // Normalizar entrada para separar URLs pegadas sin espacios (ej: https://...https://...)
        String normalizedInput = input.replaceAll("(?i)(https?://)", " $1").trim();
        String[] urls = normalizedInput.split("[\\s,]+");
        boolean addedAny = false;

        for (String urlStr : urls) {
            String url = urlStr.trim();
            if (url.isEmpty()) continue;

            if (!isValidUrl(url)) {
                LOGGER.warn("URL inválida ignorada: {}", url);
                continue;
            }

            if (queueManager.contains(url)) {
                LOGGER.warn("URL duplicada ignorada: {}", url);
                continue;
            }

            if (queueManager.addToQueue(url)) {
                LOGGER.info("URL agregada a la cola: {}", url);
                addedAny = true;
            }
        }

        if (addedAny) {
            inputField.clear();
            refreshQueue();
            if (eventPublisher != null) {
                eventPublisher.publish(new com.example.interfaz.event.DownloadEvent.QueueUpdated());
            }
        } else {
            dialogService.showError("URL inválida", "Las URLs ingresadas no son válidas o ya están en la cola.");
        }
    }

    @FXML
    public void handleClearQueue() {
        if (queueManager.isEmpty()) {
            dialogService.showInfo("Cola vacía", "No hay elementos en la cola para limpiar.");
            return;
        }

        dialogService.showConfirmation(
            "Confirmar limpieza",
            "¿Estás seguro?",
            "Se eliminarán todos los elementos de la cola.",
            () -> {
                int removedCount = queueManager.size();
                queueManager.clearQueue();
                Platform.runLater(queueItems::clear);
                LOGGER.info("Cola limpiada: {} elementos removidos", removedCount);
            }
        );
    }

    @FXML
    public void handleRemoveSelected() {
        if (queueListView == null) return;
        String selectedItem = queueListView.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            dialogService.showError("Sin selección", "Por favor selecciona un elemento de la cola para remover.");
            return;
        }

        if (queueManager.removeFromQueue(selectedItem)) {
            Platform.runLater(() -> queueItems.remove(selectedItem));
            LOGGER.info("Elemento removido de la cola: {}", selectedItem);
        }
    }

    private boolean isValidUrl(String url) {
        if (filterService != null) {
            return filterService.isValidUrl(url);
        }
        return url.contains("youtube.com") || url.contains("youtu.be") || 
               url.contains("spotify.com") || url.contains("soundcloud.com");
    }

    public void updateQueueCount() {
        Platform.runLater(() -> {
            if (queueManager != null && queueCountLabel != null) {
                int count = queueManager.size();
                queueCountLabel.setText("Elementos en cola: " + count);
                if (clearQueueButton != null) {
                    clearQueueButton.setDisable(count == 0);
                }
            }
        });
    }

    public boolean isQueueEmpty() {
        return queueManager == null || queueManager.isEmpty();
    }

    public int getQueueSize() {
        return queueManager != null ? queueManager.size() : 0;
    }

    public void setControlsEnabled(boolean enabled) {
        Platform.runLater(() -> {
            if (addButton != null) addButton.setDisable(!enabled);
            if (clearQueueButton != null) clearQueueButton.setDisable(!enabled || isQueueEmpty());
            if (removeSelectedButton != null && queueListView != null) {
                removeSelectedButton.setDisable(!enabled || queueListView.getSelectionModel().getSelectedItem() == null);
            }
            if (inputField != null) inputField.setDisable(!enabled);
        });
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }
}
