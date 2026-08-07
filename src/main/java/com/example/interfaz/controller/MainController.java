package com.example.interfaz.controller;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.YouTubeDownloadService;
import com.example.interfaz.service.download.DownloadCoordinator;
import com.example.interfaz.service.download.DownloadProgressParser;
import com.example.interfaz.service.ui.DialogService;
import com.example.interfaz.service.ui.FolderChooserService;
import com.example.interfaz.service.ui.NavigationService;
import com.example.interfaz.service.ui.ThemeService;
import com.example.interfaz.service.ui.WindowManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@SuppressWarnings({ "unused", "FXML" })
public class MainController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    // Views automatically included by FXMLLoader via <fx:include>
    @FXML
    VBox queueView;
    @FXML
    QueueController queueViewController;

    @FXML
    VBox progressView;
    @FXML
    ProgressController progressViewController;

    @FXML
    VBox logsSection;

    @FXML
    VBox libraryAnalyzerView;

    // Header and sidebar components
    @FXML
    FontIcon themeIcon;
    @FXML
    Button btnNavQueue;
    @FXML
    Button btnNavDownloads;
    @FXML
    Button btnNavLogs;
    @FXML
    Button btnNavAnalyzer;
    @FXML
    TextField inputField;
    @FXML
    Label musicFolderLabel;
    @FXML
    Label ytDlpStatusLabel;
    @FXML
    Label ytDlpVersionLabel;
    @FXML
    Button updateYtDlpButton;

    // Services injected from ServiceFactory
    private NavigationService navigationService;
    private ThemeService themeService;
    private DialogService dialogService;
    private WindowManager windowManager;
    private FolderChooserService folderChooserService;
    private DownloadProgressParser progressParser;
    private com.example.interfaz.service.download.YtDlpUpdateService ytDlpUpdateService;
    private com.example.interfaz.service.update.UpdateInfo lastUpdateInfo;

    private DownloadCoordinator downloadCoordinator;
    private DownloadService downloadService;
    private Stage primaryStage;

    @FXML
    void initialize() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();
            injectServices(serviceFactory);

            if (downloadService instanceof YouTubeDownloadService ytService) {
                ytService.setProgressCallback(progressParser::parseAndDispatch);
            }

            initializeCoordinator(serviceFactory);
            setupEventSubscriptions(serviceFactory);

            if (progressViewController != null) {
                progressParser.setListener(progressViewController);
                progressViewController.setPauseAction(downloadCoordinator::pauseDownload);
                progressViewController.setResumeAction(downloadCoordinator::resumeDownload);
                progressViewController.setCancelAction(() -> {
                    downloadCoordinator.cancelDownload();
                    progressViewController.markDownloadCancelled();
                    progressViewController.togglePauseResumeButtons(false);
                    if (queueViewController != null)
                        queueViewController.setControlsEnabled(true);
                });
            }

            if (musicFolderLabel != null) {
                musicFolderLabel.setText(folderChooserService.getDisplayPath());
            }
            loadYtDlpVersion();
            LOGGER.info("MainController ultra-delgado e impulsado por eventos inicializado correctamente");
        } catch (Exception e) {
            LOGGER.error("Error inicializando MainController", e);
        }
    }

    private void loadYtDlpVersion() {
        if (ytDlpVersionLabel != null && ytDlpUpdateService != null) {
            ytDlpUpdateService.checkUpdateAsync().thenAccept(info -> Platform.runLater(() -> {
                this.lastUpdateInfo = info;
                ytDlpVersionLabel.setText("Ver: " + info.getCurrentVersion());
                if (info.isUpdateAvailable()) {
                    if (ytDlpStatusLabel != null) {
                        ytDlpStatusLabel.setText("Estado: ⚡ Nueva v" + info.getLatestVersion());
                    }
                    if (updateYtDlpButton != null) {
                        updateYtDlpButton.setText("Actualizar ahora");
                    }
                } else {
                    if (ytDlpStatusLabel != null) {
                        ytDlpStatusLabel.setText("Estado: ✓ Al día");
                    }
                    if (updateYtDlpButton != null) {
                        updateYtDlpButton.setText("Buscar actualización");
                    }
                }
            }));
        }
    }

    private void injectServices(ServiceFactory serviceFactory) {
        this.downloadService = serviceFactory.getDownloadService();
        this.navigationService = serviceFactory.getNavigationService();
        this.themeService = serviceFactory.getThemeService();
        this.dialogService = serviceFactory.getDialogService();
        this.windowManager = serviceFactory.getWindowManager();
        this.folderChooserService = serviceFactory.getFolderChooserService();
        this.progressParser = serviceFactory.getDownloadProgressParser();
        this.ytDlpUpdateService = serviceFactory.getYtDlpUpdateService();
    }

    private void initializeCoordinator(ServiceFactory serviceFactory) {
        this.downloadCoordinator = new DownloadCoordinator(
                downloadService,
                queueViewController.getQueueManager(),
                serviceFactory.getEventPublisher());
        serviceFactory.registerDownloadCoordinator(downloadCoordinator);

        if (ytDlpUpdateService != null) {
            ytDlpUpdateService.setActiveDownloadChecker(downloadCoordinator::isDownloading);
        }
    }

    private void setupEventSubscriptions(ServiceFactory serviceFactory) {
        var eventPublisher = serviceFactory.getEventPublisher();
        if (eventPublisher != null) {
            eventPublisher.subscribe(DownloadEvent.StateChanged.class, event -> Platform.runLater(() -> {
                if (queueViewController != null) {
                    queueViewController.setControlsEnabled(!event.isDownloading());
                }
                if (progressViewController != null) {
                    progressViewController.togglePauseResumeButtons(event.isPaused());
                }
                if (!event.isDownloading() && progressViewController != null && downloadCoordinator.isQueueEmpty()) {
                    progressViewController.markDownloadCompleted();
                    progressViewController.togglePauseResumeButtons(false);
                }
            }));

            eventPublisher.subscribe(DownloadEvent.QueueEmpty.class, event -> Platform.runLater(
                    () -> dialogService.showInfo("Cola vacía", "Agrega URLs a la cola antes de iniciar la descarga.")));

            eventPublisher.subscribe(DownloadEvent.DownloadCompleted.class, event -> Platform.runLater(() -> {
                if (progressViewController != null && event.getSong() != null) {
                    progressViewController.updateStatus("✅ " + event.getSong().getTitle() + " completado");
                }
            }));
        }
    }

    // Delegaciones @FXML puras y directas
    @FXML
    void onAddToQueue() {
        if (inputField != null) {
            String url = inputField.getText();
            if (downloadCoordinator.addToQueue(url)) {
                inputField.clear();
            } else {
                dialogService.showError("URL no válida", "La URL ingresada no es válida o ya se encuentra en la cola.");
            }
        }
    }

    @FXML
    void onNavQueue() {
        navigationService.navigateTo(btnNavQueue, queueView, progressView, logsSection);
    }

    @FXML
    void onNavDownloads() {
        navigationService.navigateTo(btnNavDownloads, progressView, queueView, logsSection);
    }

    @FXML
    void onNavLogs() {
        navigationService.navigateTo(btnNavLogs, logsSection, queueView, progressView, libraryAnalyzerView);
    }

    @FXML
    void onNavAnalyzer() {
        navigationService.navigateTo(btnNavAnalyzer, libraryAnalyzerView, queueView, progressView, logsSection);
    }

    @FXML
    void onStartDownload() {
        onNavDownloads();
        if (progressViewController != null) {
            progressViewController.showProgressSection();
            progressViewController.updateStatus("🚀 Iniciando descarga...");
            progressViewController.togglePauseResumeButtons(false);
        }
        downloadCoordinator.startDownload();
    }

    @FXML
    private void onResumeClick() {
        downloadCoordinator.resume();
        if (progressViewController != null) {
            progressViewController.togglePauseResumeButtons(false);
        }
    }

    @FXML
    void onToggleTheme() {
        themeService.toggleTheme(themeIcon);
    }

    @FXML
    void onShowLogs() {
        windowManager.showLogsWindow(primaryStage);
    }

    @FXML
    void onSelectMusicFolder() {
        String newPath = folderChooserService.promptAndSelectFolder(primaryStage);
        if (musicFolderLabel != null) {
            musicFolderLabel.setText(newPath);
        }
    }

    @FXML
    void onResetMusicFolder() {
        String newPath = folderChooserService.resetToDefaultFolder();
        if (musicFolderLabel != null) {
            musicFolderLabel.setText(newPath);
        }
    }

    @FXML
    void onUpdateYtDlp() {
        if (downloadCoordinator != null && downloadCoordinator.isDownloading()) {
            dialogService.showWarning("Descarga en curso", "No se puede actualizar yt-dlp mientras existen descargas activas en curso.");
            return;
        }

        if (updateYtDlpButton != null) {
            updateYtDlpButton.setDisable(true);
        }
        if (ytDlpStatusLabel != null) {
            ytDlpStatusLabel.setText("Estado: Procesando...");
        }

        if (lastUpdateInfo != null && lastUpdateInfo.isUpdateAvailable()) {
            ytDlpUpdateService.updateYtDlpAsync(logLine -> LOGGER.info("[yt-dlp update UI] {}", logLine))
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (updateYtDlpButton != null) {
                            updateYtDlpButton.setDisable(false);
                        }
                        loadYtDlpVersion();
                        if (success) {
                            dialogService.showConfirmation(
                                "Actualización completada",
                                "yt-dlp se ha actualizado correctamente.",
                                "¿Deseas reiniciar la aplicación ahora para asegurar el uso del nuevo ejecutable?",
                                Platform::exit
                            );
                        } else {
                            dialogService.showError("Error de Actualización", "No se pudo actualizar yt-dlp. Revisa los registros para más detalles.");
                        }
                    }));
        } else {
            ytDlpUpdateService.checkUpdateAsync(true).thenAccept(info -> Platform.runLater(() -> {
                this.lastUpdateInfo = info;
                if (updateYtDlpButton != null) {
                    updateYtDlpButton.setDisable(false);
                }
                loadYtDlpVersion();
                if (info.isUpdateAvailable()) {
                    dialogService.showInfo("Actualización disponible", "Nueva versión disponible: " + info.getLatestVersion() + ".\nHaz clic en 'Actualizar ahora' para continuar.");
                } else {
                    dialogService.showInfo("yt-dlp al día", "Ya cuentas con la versión más reciente de yt-dlp (" + info.getCurrentVersion() + ").");
                }
            }));
        }
    }

    public QueueController getQueueController() {
        return queueViewController;
    }

    public ProgressController getProgressController() {
        return progressViewController;
    }

    public void setStage(Stage stage) {
        this.primaryStage = stage;
    }
}
