package com.example.interfaz.controller;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.event.DownloadEvent;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.download.MainDownloadFacade;
import com.example.interfaz.service.ui.UIFacade;
import com.example.interfaz.viewmodel.MainViewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

@SuppressWarnings({"unused", "FXML"})
public class MainController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootNode;

    @FXML private Button btnNavQueue;
    @FXML private Button btnNavDownloads;
    @FXML private Button btnNavLogs;
    @FXML private Button btnNavAnalyzer;

    @FXML private Label musicFolderLabel;
    @FXML private Button selectFolderButton;
    @FXML private Button resetFolderButton;

    @FXML private Label ytDlpVersionLabel;
    @FXML private Label ytDlpStatusLabel;
    @FXML private Button updateYtDlpButton;

    @FXML private Button themeToggleButton;
    @FXML private FontIcon themeIcon;

    @FXML private TextField inputField;
    @FXML private Button addButton;
    @FXML private Button startButton;

    @FXML private Node queueView;
    @FXML private Node progressView;
    @FXML private Node logsSection;
    @FXML private Node libraryAnalyzerView;

    @FXML private QueueController queueViewController;
    @FXML private ProgressController progressViewController;

    private UIFacade uiFacade;
    private MainViewModel mainViewModel;
    private MainDownloadFacade downloadFacade;
    private Stage primaryStage;

    @FXML
    public void initialize() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.uiFacade = factory.getUIFacade();
        this.mainViewModel = factory.getMainViewModel();
        this.downloadFacade = factory.getMainDownloadFacade();

        setupFolderManager();
        setupYtDlpUpdater();
        setupDownloadFacade();

        LOGGER.info("MainController inicializado.");
    }

    public void setStage(Stage stage) {
        this.primaryStage = stage;
        if (uiFacade != null && uiFacade.getWindowManager() != null) {
            uiFacade.getWindowManager().setPrimaryStage(stage);
        }
    }

    public QueueController getQueueController() {
        return queueViewController;
    }

    public ProgressController getProgressController() {
        return progressViewController;
    }

    private void setupFolderManager() {
        var musicFolderService = ServiceFactory.getInstance().getMusicFolderService();

        mainViewModel.musicFolderDisplayPathProperty().addListener((obs, oldVal, newVal) -> {
            if (musicFolderLabel != null) musicFolderLabel.setText(newVal);
        });

        mainViewModel.updateMusicFolderDisplay(musicFolderService.getMusicFolderPath());
    }

    private void setupYtDlpUpdater() {
        if (ytDlpVersionLabel != null) {
            ytDlpVersionLabel.textProperty().bind(mainViewModel.ytDlpVersionTextProperty());
        }
        if (ytDlpStatusLabel != null) {
            ytDlpStatusLabel.textProperty().bind(mainViewModel.ytDlpStatusTextProperty());
        }
        if (updateYtDlpButton != null) {
            updateYtDlpButton.textProperty().bind(mainViewModel.updateButtonTextProperty());
            updateYtDlpButton.disableProperty().bind(mainViewModel.updateButtonDisabledProperty());
        }
    }

    private void setupDownloadFacade() {
        downloadFacade.initialize(queueViewController, progressViewController, () -> {
            LOGGER.info("Cola de descargas vacía.");
        });

        downloadFacade.subscribeToEvents(
                this::onDownloadQueued,
                this::onDownloadStarted,
                this::onDownloadProgress,
                this::onDownloadCompleted,
                this::onDownloadError
        );
    }

    @FXML
    void onAddToQueue() {
        if (inputField == null) return;
        String url = inputField.getText().trim();
        if (url.isEmpty()) {
            uiFacade.getDialogService().showWarning("URL Vacía", "Por favor introduce una URL válida de YouTube.");
            return;
        }

        boolean success = downloadFacade.addToQueue(url);
        if (success) {
            inputField.clear();
        } else {
            uiFacade.getDialogService().showError("Error al encolar", "No se pudo encolar la descarga. Verifica la URL o la configuración.");
        }
    }

    @FXML
    void onNavQueue() {
        uiFacade.getNavigationService().navigateTo(btnNavQueue, queueView, progressView, logsSection, libraryAnalyzerView);
    }

    @FXML
    void onNavDownloads() {
        uiFacade.getNavigationService().navigateTo(btnNavDownloads, progressView, queueView, logsSection, libraryAnalyzerView);
    }

    @FXML
    void onNavLogs() {
        uiFacade.getNavigationService().navigateTo(btnNavLogs, logsSection, queueView, progressView, libraryAnalyzerView);
    }

    @FXML
    void onNavAnalyzer() {
        uiFacade.getNavigationService().navigateTo(btnNavAnalyzer, libraryAnalyzerView, queueView, progressView, logsSection);
    }

    @FXML
    void onStartDownload() {
        onNavDownloads();
        if (progressViewController != null) {
            progressViewController.showProgressSection();
            progressViewController.updateStatus("🚀 Iniciando descarga...");
            progressViewController.togglePauseResumeButtons(false);
        }
        downloadFacade.startNextDownload();
    }

    @FXML
    void onToggleTheme() {
        uiFacade.getThemeService().toggleTheme(themeIcon);
    }

    @FXML
    void onSelectMusicFolder() {
        String newPath = uiFacade.getFolderChooserService().promptAndSelectFolder(primaryStage);
        if (musicFolderLabel != null) {
            musicFolderLabel.setText(newPath);
        }
        mainViewModel.updateMusicFolderDisplay(newPath);
    }

    @FXML
    void onResetMusicFolder() {
        String newPath = uiFacade.getFolderChooserService().resetToDefaultFolder();
        if (musicFolderLabel != null) {
            musicFolderLabel.setText(newPath);
        }
        mainViewModel.updateMusicFolderDisplay(newPath);
    }

    @FXML
    void onUpdateYtDlp() {
        var updateService = ServiceFactory.getInstance().getYtDlpUpdateService();
        mainViewModel.checkAndPerformYtDlpUpdate(updateService, uiFacade.getDialogService());
    }

    private void onDownloadQueued(DownloadEvent.DownloadStarted event) {
        LOGGER.info("Evento capturado: Descarga encolada [{}]", event.getSong() != null ? event.getSong().getTitle() : "");
    }

    private void onDownloadStarted(DownloadEvent.DownloadStarted event) {
        LOGGER.info("Evento capturado: Descarga iniciada [{}]", event.getSong() != null ? event.getSong().getTitle() : "");
    }

    private void onDownloadProgress(DownloadEvent.DownloadProgress event) {
    }

    private void onDownloadCompleted(DownloadEvent.DownloadCompleted event) {
        LOGGER.info("Evento capturado: Descarga completada [{}]", event.getSong() != null ? event.getSong().getTitle() : "");
    }

    private void onDownloadError(DownloadEvent.DownloadFailed event) {
        LOGGER.error("Evento capturado: Error en descarga [{}] - {}", event.getSong() != null ? event.getSong().getTitle() : "", event.getError());
        Platform.runLater(() -> uiFacade.getDialogService().showError("Error de Descarga",
                "Ocurrió un error al descargar '" + (event.getSong() != null ? event.getSong().getTitle() : "") + "': " + event.getError()));
    }

    @Override
    public void close() {
        if (downloadFacade != null) {
            downloadFacade.unsubscribeFromEvents();
            downloadFacade.close();
        }
        LOGGER.info("MainController liberado.");
    }
}
