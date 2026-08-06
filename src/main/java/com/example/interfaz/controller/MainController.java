package com.example.interfaz.controller;

import java.io.File;
import java.io.IOException;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.config.ConfigurationManager;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.LogService;
import com.example.interfaz.service.YouTubeDownloadService;
import com.example.interfaz.service.config.MusicFolderService;
import com.example.interfaz.service.download.DownloadProgressParser;
import com.example.interfaz.util.FileUtils;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

@SuppressWarnings("unused")
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    private QueueController queueController;
    private ProgressController progressController;

    @FXML private BorderPane rootNode;
    @FXML private FontIcon themeIcon;
    @FXML private Button themeToggleButton;
    private boolean isDarkMode = true;

    @FXML private Button btnNavQueue;
    @FXML private Button btnNavDownloads;
    @FXML private Button btnNavLogs;

    @FXML private TextField inputField;
    @FXML private Button addButton;

    @FXML private VBox queueSection;
    @FXML private Label queueCountLabel;
    @FXML private Button clearQueueButton;
    @FXML private Button removeSelectedButton;
    @FXML private ListView<String> queueListView;

    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private Button cancelButton;
    @FXML private Button showLogsButton;
    @FXML private Button clearSongsFileButton;

    @FXML private Label musicFolderLabel;
    @FXML private Button selectFolderButton;
    @FXML private Button resetFolderButton;

    @FXML private VBox progressSection;
    @FXML private VBox logsSection;
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

    private static MainController instance;

    public MainController() {
        instance = this;
    }

    @FXML
    private void initialize() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();

            this.downloadService = serviceFactory.getDownloadService();

            if (downloadService instanceof YouTubeDownloadService ytService) {
                ytService.setProgressCallback(this::handleProgressUpdate);
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
    private void onNavQueue() {
        setActiveSection(btnNavQueue, queueSection);
    }

    @FXML
    private void onNavDownloads() {
        setActiveSection(btnNavDownloads, progressSection);
    }

    @FXML
    private void onNavLogs() {
        setActiveSection(btnNavLogs, logsSection);
    }

    private void setActiveSection(Button activeButton, VBox activeSection) {
        if (btnNavQueue != null) btnNavQueue.getStyleClass().remove("active");
        if (btnNavDownloads != null) btnNavDownloads.getStyleClass().remove("active");
        if (btnNavLogs != null) btnNavLogs.getStyleClass().remove("active");

        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }

        if (queueSection != null) {
            boolean isQueue = (queueSection == activeSection);
            queueSection.setVisible(isQueue);
            queueSection.setManaged(isQueue);
        }
        if (progressSection != null) {
            boolean isProgress = (progressSection == activeSection);
            progressSection.setVisible(isProgress);
            progressSection.setManaged(isProgress);
        }
        if (logsSection != null) {
            boolean isLogs = (logsSection == activeSection);
            logsSection.setVisible(isLogs);
            logsSection.setManaged(isLogs);
        }
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
        onNavDownloads();
        progressController.showProgressSection();
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

    private final DownloadProgressParser progressParser = new DownloadProgressParser();
    private final MusicFolderService musicFolderService = new MusicFolderService();

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void handleProgressUpdate(String message) {
        progressParser.parseAndDispatch(message, new DownloadProgressParser.ProgressListener() {
            @Override
            public void onOverallProgress(int current, int total) {
                progressController.updateOverallProgress(current, total);
            }

            @Override
            public void onSongStart(String songTitle) {
                progressController.updateCurrentSong(songTitle);
            }

            @Override
            public void onCurrentProgress(double progress, String statusText) {
                progressController.updateCurrentProgress(progress, statusText);
            }

            @Override
            public void onSpeedUpdate(String speed) {
                progressController.updateDownloadSpeed(speed);
            }

            @Override
            public void onEtaUpdate(String eta) {
                progressController.updateETA(eta);
            }

            @Override
            public void onStatusUpdate(String statusMessage) {
                progressController.updateStatus(statusMessage);
            }

            @Override
            public void onGenericMessage(String message) {
                progressController.handleProgressUpdate(message);
            }
        });
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

    public void setStage(Stage stage) {
        this.primaryStage = stage;
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

    @FXML
    private void onSelectMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta de Música");

        File currentDir = new File(musicFolderService.getCurrentMusicFolder());
        if (currentDir.exists()) {
            directoryChooser.setInitialDirectory(currentDir);
        }

        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            String newPath = musicFolderService.setMusicFolder(selectedDirectory.getAbsolutePath());
            updateMusicFolderLabel();
            showAlert("Carpeta Actualizada", "La carpeta de música se ha cambiado a: " + newPath);
        }
    }

    @FXML
    private void onResetMusicFolder() {
        String defaultPath = musicFolderService.resetToDefaultMusicFolder();
        updateMusicFolderLabel();
        showAlert("Carpeta Restablecida", "La carpeta de música se ha restablecido a: " + defaultPath);
    }

    private void updateMusicFolderLabel() {
        if (musicFolderLabel != null) {
            musicFolderLabel.setText(musicFolderService.getDisplayPath());
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

    @FXML
    private void onToggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            if (themeIcon != null) {
                themeIcon.setIconLiteral("mdi2m-moon-waning-crescent");
            }
            LOGGER.info("Cambiado a Modo Oscuro (Primer Dark)");
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            if (themeIcon != null) {
                themeIcon.setIconLiteral("mdi2w-weather-sunny");
            }
            LOGGER.info("Cambiado a Modo Claro (Primer Light)");
        }
    }
}
