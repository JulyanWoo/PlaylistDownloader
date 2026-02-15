package com.example.interfaz.controller;

import com.example.interfaz.service.*;
import com.example.interfaz.service.YouTubeDownloadService;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.config.ConfigurationManager;
import com.example.interfaz.util.FileUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador principal coordinador de la interfaz JavaFX
 * Delega responsabilidades específicas a controladores especializados
 */
public class MainController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);
    
    private QueueController queueController;
    private ProgressController progressController;
    
    // Input elements
    @FXML private TextField inputField;
    @FXML private Button addButton;
    
    // Queue elements
    @FXML private VBox queueSection;
    @FXML private Label queueCountLabel;
    @FXML private Button clearQueueButton;
    @FXML private Button removeSelectedButton;
    @FXML private ListView<String> queueListView;
    
    // Control buttons
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button cancelButton;
    @FXML private Button showLogsButton;
    @FXML private Button clearSongsFileButton;
    
    // Configuration elements
    @FXML private Label musicFolderLabel;
    @FXML private Button selectFolderButton;
    @FXML private Button resetFolderButton;
    
    // Progress elements
    @FXML private VBox progressSection;
    @FXML private Label currentSongLabel;
    @FXML private Label overallProgressLabel;
    @FXML private Label overallPercentageLabel;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label currentProgressLabel;
    @FXML private Label currentPercentageLabel;
    @FXML private ProgressBar currentProgressBar;
    @FXML private Label downloadSpeedLabel;


    private UIStateManager uiStateManager;
    private ProgressManager progressManager;
    private EventHandler eventHandler;
    private Task<Void> downloadTask;
    
    private DownloadService downloadService;
    private Stage primaryStage;
    
    // Instancia singleton para compatibilidad
    private static MainController instance;
    
    public MainController() {
        instance = this;
    }
    
    @FXML
    private void initialize() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();
            
            this.downloadService = serviceFactory.getDownloadService();
            
            if (downloadService instanceof YouTubeDownloadService) {
                ((YouTubeDownloadService) downloadService).setProgressCallback(this::handleProgressUpdate);
            }
            
            initializeSubControllers();
            initializeManagers(serviceFactory);
            setInitialState();
            
            LOGGER.info("MainController inicializado correctamente como coordinador");
        } catch (Exception e) {
            LOGGER.error("Error inicializando MainController", e);
        }
    }
    
    private void initializeSubControllers() {
        queueController = new QueueController();
        progressController = new ProgressController();
        
        queueController.setComponents(inputField, queueListView, queueCountLabel, 
                                    addButton, clearQueueButton, removeSelectedButton);
        progressController.setComponents(progressSection, currentSongLabel, 
                                       overallProgressLabel, overallPercentageLabel, overallProgressBar,
                                       currentProgressLabel, currentPercentageLabel, currentProgressBar);
        
        queueController.initialize(this);
        progressController.initialize(this);
        
        LOGGER.info("Subcontroladores inicializados correctamente");
    }
    
    private void initializeManagers(ServiceFactory serviceFactory) {
        createStateManagers(serviceFactory);
        createEventHandler(serviceFactory);
        configureInitialStates();
        
        // Inicializar la etiqueta de la carpeta de música
        updateMusicFolderLabel();
        
        LOGGER.info("Gestores de estado inicializados");
    }
    
    private void createStateManagers(ServiceFactory serviceFactory) {
        this.uiStateManager = new UIStateManager(
            inputField, addButton, startButton, pauseButton, cancelButton,
            clearQueueButton, removeSelectedButton, queueListView
        );
        this.progressManager = serviceFactory.createProgressManager();
    }
    
    private void createEventHandler(ServiceFactory serviceFactory) {
        this.eventHandler = new EventHandler(
            this, downloadService, uiStateManager, 
            progressManager, queueController != null ? queueController.getQueueManager() : null, 
            serviceFactory.getEventPublisher(), null, null
        );
    }
    
    private void configureInitialStates() {
        uiStateManager.setDownloadingState(false);
        uiStateManager.setPausedState(false);
        uiStateManager.setShouldStop(false);
    }
    
    private void setInitialState() {
        uiStateManager.setDownloadingState(false);
        
        Platform.runLater(() -> {
            progressController.hideProgressSection();
            progressController.updateStatus("✅ Listo para descargar");
            
            queueController.setControlsEnabled(true);
        });
        
        LOGGER.info("Estado inicial configurado");
    }
    
    @FXML
    private void onAddToQueue() {
        queueController.handleAddToQueue();
    }
    
    @FXML
    private void onClearQueue() {
        queueController.handleClearQueue();
    }
    
    @FXML
    private void onRemoveSelected() {
        queueController.handleRemoveSelected();
    }
    
    @FXML
    private void onStartDownload() {
        if (queueController.isQueueEmpty()) {
            showAlert("Cola vacía", "Agrega URLs a la cola antes de iniciar la descarga.");
            return;
        }
        startDownloadProcess();
    }
    
    private void startDownloadProcess() {
        progressController.showProgressSection();
        adjustWindowSizeForDownload();
        progressController.updateStatus("🚀 Iniciando descarga...");
        
        downloadTask = eventHandler.createDownloadTask();
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
        
        uiStateManager.setDownloadingState(true);
        LOGGER.info("Proceso de descarga iniciado");
    }
    
    @FXML
    private void onPauseDownload() {
        if (!uiStateManager.isDownloading()) return;
        
        if (uiStateManager.isPaused()) {
            resumeDownload();
        } else {
            pauseDownload();
        }
    }
    
    private void pauseDownload() {
        uiStateManager.setPausedState(true);
        downloadService.pauseDownload();
        progressController.markDownloadPaused();
        LOGGER.info("Descarga pausada por el usuario");
    }
    
    private void resumeDownload() {
        uiStateManager.setPausedState(false);
        downloadService.resumeDownload();
        progressController.updateStatus("▶️ Descarga reanudada");
        LOGGER.info("Descarga reanudada por el usuario");
    }
    
    @FXML
    private void onCancelDownload() {
        if (!uiStateManager.isDownloading()) return;
        
        cancelDownloadProcess();
    }
    
    private void cancelDownloadProcess() {
        uiStateManager.setShouldStop(true);
        uiStateManager.setPausedState(false);
        
        downloadService.stopDownload();
        
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
        
        Platform.runLater(() -> {
            uiStateManager.setDownloadingState(false);
            progressController.markDownloadCancelled();
            queueController.setControlsEnabled(true);
        });
        
        LOGGER.info("Descarga cancelada por el usuario");
    }
    
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void handleProgressUpdate(String message) {
        if (message.startsWith("PLAYLIST_PROGRESS:")) {
            String progressInfo = message.substring("PLAYLIST_PROGRESS:".length());
            String[] parts = progressInfo.split("/");
            if (parts.length == 2) {
                try {
                    int currentItem = Integer.parseInt(parts[0]);
                    int totalItems = Integer.parseInt(parts[1]);
                    progressController.updateOverallProgress(currentItem, totalItems);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Error parseando progreso de playlist: {}", message);
                }
            }
        } else if (message.startsWith("SONG_START:")) {
            String songInfo = message.substring("SONG_START:".length());
            progressController.updateCurrentSong(songInfo);
        } else if (message.startsWith("PROGRESS:")) {
            String percentageStr = message.substring("PROGRESS:".length());
            try {
                double percentage = Double.parseDouble(percentageStr);
                double progress = percentage / 100.0;
                progressController.updateCurrentProgress(progress, String.format("Descargando... %.1f%%", percentage));
            } catch (NumberFormatException e) {
                LOGGER.warn("Error parseando porcentaje: {}", message);
            }
        } else if (message.startsWith("SPEED:")) {
            String speed = message.substring("SPEED:".length());
            progressController.updateDownloadSpeed(speed);
        } else if (message.startsWith("ETA:")) {
            String eta = message.substring("ETA:".length());
            progressController.updateETA(eta);
        } else if (message.startsWith("DOWNLOADING:")) {
            String videoTitle = message.substring("DOWNLOADING:".length());
            progressController.updateCurrentSong(videoTitle);
            progressController.updateStatus("🎵 Descargando...");
        } else if (message.startsWith("COMPLETED:") || message.startsWith("PROCESSED:")) {
            String completedTitle = message.substring(message.indexOf(":") + 1);
            progressController.updateStatus("✅ Completado: " + completedTitle);
        } else if (!message.startsWith("DOWNLOAD_START:") && 
                   !message.contains("[youtube:tab]") && 
                   !message.contains("[youtube]") &&
                   !message.contains("[download]") &&
                   !message.startsWith("Iniciando descarga") &&
                   !message.contains("Downloading item") &&
                   !message.contains("API JSON") &&
                   !message.contains("player API") &&
                   !message.contains("ios player")) {
            progressController.handleProgressUpdate(message);
        }
    }
    
    public void updateCurrentProgress(double progress, String details) {
        progressController.updateCurrentProgress(progress, details);
    }
    
    public void updateDownloadInfo(String speed, String eta) {
        progressController.updateDownloadInfo(speed, eta);
    }
    
    public void updatePlaylistProgress(int currentItem, int totalItems) {
        progressController.updatePlaylistProgress(currentItem, totalItems);
    }
    
    public static MainController getInstance() {
        return instance;
    }
    
    public void onProgressUpdate(String message, double progress) {
        Platform.runLater(() -> progressManager.updateCurrentProgress(message, progress));
        queueController.updateQueueCount();
    }
    
    public void onSpeedUpdate(String speed, String eta) {
        Platform.runLater(() -> progressManager.updateSpeedAndETA(speed, eta));
    }
    
    public boolean shouldStop() {
        return uiStateManager.shouldStop();
    }
    
    public boolean isPaused() {
        return uiStateManager.isPaused();
    }
    
    public boolean isDownloading() {
        return uiStateManager.isDownloading();
    }
    
    public static boolean shouldStopStatic() {
        return instance != null && instance.uiStateManager.shouldStop();
    }
    
    public static boolean isPausedStatic() {
        return instance != null && instance.uiStateManager.isPaused();
    }
    
    public static boolean isDownloadingStatic() {
        return instance != null && instance.uiStateManager.isDownloading();
    }
    public QueueController getQueueController() {
        return queueController;
    }
    
    public ProgressController getProgressController() {
        return progressController;
    }
    
    public UIStateManager getUIStateManager() {
        return uiStateManager;
    }
    
    public DownloadService getDownloadService() {
        return downloadService;
    }
    
    /**
     * Establece la referencia al Stage principal para poder ajustar el tamaño de la ventana
     */
    public void setStage(Stage stage) {
        this.primaryStage = stage;
    }
    
    /**
     * Ajusta automáticamente el tamaño de la ventana cuando se inicia la descarga
     */
    private void adjustWindowSizeForDownload() {
        if (primaryStage != null) {
            Platform.runLater(() -> {
                double currentHeight = primaryStage.getHeight();
                double newHeight = Math.max(currentHeight, 700); 
                
                primaryStage.setHeight(newHeight);
                
                LOGGER.debug("Tamaño de ventana ajustado para mostrar progreso: {}x{}", primaryStage.getWidth(), newHeight);
            });
        }
    }
    
    @FXML
    private void onShowLogs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/logs-view.fxml"));
            Parent root = loader.load();
            
            LogsController logsController = loader.getController();
            
            Stage logsStage = new Stage();
            logsStage.setTitle("📋 Logs de la Consola - YouTube Downloader");
            logsStage.initModality(Modality.NONE);
            logsStage.initOwner(primaryStage);
            
            Scene scene = new Scene(root);
            logsStage.setScene(scene);
            
            logsController.setStage(logsStage);
            
            logsStage.setResizable(true);
            logsStage.setMinWidth(600);
            logsStage.setMinHeight(400);
            
            logsStage.show();
            
            LogService.log("Ventana de logs abierta por el usuario");
            
        } catch (IOException e) {
            LOGGER.error("Error al abrir la ventana de logs", e);
            showAlert("Error", "No se pudo abrir la ventana de logs: " + e.getMessage());
        }
    }
    
    /**
     * Maneja la selección de una nueva carpeta de música
     */
    @FXML
    private void onSelectMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta de Música");
        
        // Establecer directorio inicial como el actual
        File currentDir = new File(FileUtils.getMusicDirectory());
        if (currentDir.exists()) {
            directoryChooser.setInitialDirectory(currentDir);
        }
        
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            String newPath = selectedDirectory.getAbsolutePath();
            FileUtils.setMusicDirectory(newPath);
            ConfigurationManager.getInstance().setMusicDirectory(newPath);
            ConfigurationManager.getInstance().saveConfiguration();
            
            // Actualizar la etiqueta en la interfaz
            updateMusicFolderLabel();
            
            showAlert("Carpeta Actualizada", "La carpeta de música se ha cambiado a: " + newPath);
            LOGGER.info("Carpeta de música cambiada a: {}", newPath);
        }
    }
    
    /**
     * Restablece la carpeta de música al valor por defecto
     */
    @FXML
    private void onResetMusicFolder() {
        String defaultPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "MUSICA";
        FileUtils.setMusicDirectory(defaultPath);
        ConfigurationManager.getInstance().setMusicDirectory(defaultPath);
        ConfigurationManager.getInstance().saveConfiguration();
        
        // Actualizar la etiqueta en la interfaz
        updateMusicFolderLabel();
        
        showAlert("Carpeta Restablecida", "La carpeta de música se ha restablecido a: " + defaultPath);
        LOGGER.info("Carpeta de música restablecida a: {}", defaultPath);
    }
    
    /**
     * Actualiza la etiqueta que muestra la ruta actual de la carpeta de música
     */
    private void updateMusicFolderLabel() {
        if (musicFolderLabel != null) {
            String currentPath = FileUtils.getMusicDirectory();
            // Mostrar solo el nombre de la carpeta y su padre para mantener la interfaz limpia
            File currentDir = new File(currentPath);
            String displayPath = currentDir.getParent() != null ? 
                new File(currentDir.getParent()).getName() + "/" + currentDir.getName() : 
                currentDir.getName();
            musicFolderLabel.setText(displayPath);
        }
    }
    
    @FXML
    private void onClearSongsFile() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Limpieza");
        alert.setHeaderText("Limpiar archivo de canciones descargadas");
        alert.setContentText("Esto eliminará el archivo actual que contiene números y creará uno nuevo que guardará correctamente los nombres de las canciones.\n\n¿Desea continuar?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    com.example.interfaz.util.FileUtils.clearDownloadedSongsFile();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Éxito");
                    successAlert.setHeaderText("Archivo limpiado correctamente");
                    successAlert.setContentText("El archivo de canciones descargadas ha sido reinicializado.\nAhora guardará correctamente los nombres de las canciones en lugar de números.");
                    successAlert.showAndWait();
                    
                    LOGGER.info("Archivo de canciones descargadas limpiado por el usuario");
                    
                } catch (Exception e) {
                    LOGGER.error("Error al limpiar archivo de canciones", e);
                    showAlert("Error", "No se pudo limpiar el archivo: " + e.getMessage());
                }
            }
        });
    }
}