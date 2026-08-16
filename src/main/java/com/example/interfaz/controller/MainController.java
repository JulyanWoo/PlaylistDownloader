package com.example.interfaz.controller;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.download.MainDownloadFacade;
import com.example.interfaz.service.ui.MainViewBinder;
import com.example.interfaz.service.ui.UIFacade;
import com.example.interfaz.viewmodel.MainViewModel;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@SuppressWarnings({"unused", "FXML"})
public class MainController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane rootNode;

    @FXML
    private Button btnNavWelcome;
    @FXML
    private Button btnNavDashboard;
    @FXML
    private Button btnNavLogs;
    @FXML
    private Button btnNavAnalyzer;

    @FXML
    private Label musicFolderLabel;
    @FXML
    private Button selectFolderButton;
    @FXML
    private Button resetFolderButton;

    @FXML
    private Label ytDlpVersionLabel;
    @FXML
    private Label ytDlpStatusLabel;
    @FXML
    private Button updateYtDlpButton;

    @FXML
    private Button themeToggleButton;
    @FXML
    private FontIcon themeIcon;

    @FXML
    private TextField inputField;
    @FXML
    private Button addButton;
    @FXML
    private Button startButton;

    @FXML
    private Node welcomeView;
    @FXML
    private Node queueView;
    @FXML
    private Node progressView;
    @FXML
    private Node logsSection;
    @FXML
    private Node libraryAnalyzerView;

    @FXML
    private HBox topBarContainer;
    @FXML
    private VBox sidebar;
    @FXML
    private Label appTitleLabel;
    @FXML
    private VBox sidebarConfigCard;

    @FXML
    private QueueController queueViewController;
    @FXML
    private ProgressController progressViewController;
    @FXML
    private WelcomeController welcomeViewController;

    @FXML
    private HBox customTitleBar;
    @FXML
    private Button btnMinimize;
    @FXML
    private Button btnMaximize;
    @FXML
    private Button btnClose;
    @FXML
    private FontIcon iconMaximize;

    private UIFacade uiFacade;
    private MainViewModel mainViewModel;
    private MainDownloadFacade downloadFacade;
    private Stage primaryStage;

    private final MainViewBinder mainViewBinder = new MainViewBinder();

    @FXML
    public void initialize() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.uiFacade = factory.getUIFacade();
        this.mainViewModel = factory.getMainViewModel();
        this.downloadFacade = factory.getMainDownloadFacade();

        if (welcomeViewController != null) {
            welcomeViewController.setMainController(this);
        }

        mainViewBinder.bindFolderManager(mainViewModel, musicFolderLabel, factory.getMusicFolderService());
        mainViewBinder.bindYtDlpUpdater(mainViewModel, ytDlpVersionLabel, ytDlpStatusLabel, updateYtDlpButton);

        if (downloadFacade != null) {
            downloadFacade.initialize(queueViewController, progressViewController, () -> LOGGER.info("Cola de descargas vacía."));
        }

        LOGGER.info("MainController inicializado correctamente.");
    }

    public void setStage(Stage stage) {
        this.primaryStage = stage;
        if (uiFacade != null && uiFacade.getWindowManager() != null) {
            uiFacade.getWindowManager().initCustomStage(stage, customTitleBar, rootNode, iconMaximize);
        }
    }

    public QueueController getQueueController() {
        return queueViewController;
    }

    public ProgressController getProgressController() {
        return progressViewController;
    }

    @FXML
    void onAddToQueue() {
        if (inputField == null) {
            return;
        }
        String input = inputField.getText().trim();
        if (input.isEmpty()) {
            uiFacade.getDialogService().showWarning("URL Vacía", "Por favor introduce una URL válida de YouTube.");
            return;
        }

        String normalizedInput = input.replaceAll("(?i)(https?://)", " $1").trim();
        String[] urls = normalizedInput.split("[\\s,]+");
        boolean addedAny = false;

        for (String urlStr : urls) {
            String url = urlStr.trim();
            if (url.isEmpty()) continue;
            if (downloadFacade.addToQueue(url)) {
                addedAny = true;
            }
        }

        if (addedAny) {
            inputField.clear();
        } else {
            uiFacade.getDialogService().showError("Error al encolar", "No se pudo encolar la descarga. Verifica la URL o la configuración.");
        }
    }

    private void showTopBar(boolean show) {
        if (topBarContainer != null) {
            topBarContainer.setVisible(show);
            topBarContainer.setManaged(show);
        }
    }

    @FXML
    void onNavWelcome() {
        showTopBar(false);
        uiFacade.getNavigationService().navigateTo(btnNavWelcome, welcomeView, progressView, queueView, logsSection, libraryAnalyzerView);
    }

    @FXML
    void onNavQueue() {
        showTopBar(true);
        uiFacade.getNavigationService().navigateTo(btnNavDashboard, queueView, progressView, logsSection, libraryAnalyzerView, welcomeView);
    }

    @FXML
    void onNavDownloads() {
        showTopBar(true);
        uiFacade.getNavigationService().navigateTo(btnNavDashboard, progressView, queueView, logsSection, libraryAnalyzerView, welcomeView);
    }

    @FXML
    void onNavLogs() {
        showTopBar(true);
        uiFacade.getNavigationService().navigateTo(btnNavLogs, logsSection, queueView, progressView, libraryAnalyzerView, welcomeView);
    }

    @FXML
    void onNavAnalyzer() {
        showTopBar(true);
        uiFacade.getNavigationService().navigateTo(btnNavAnalyzer, libraryAnalyzerView, queueView, progressView, logsSection, welcomeView);
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

    @FXML
    void onMinimizeWindow() {
        if (uiFacade != null && uiFacade.getWindowManager() != null) {
            uiFacade.getWindowManager().minimize();
        }
    }

    @FXML
    void onMaximizeWindow() {
        if (uiFacade != null && uiFacade.getWindowManager() != null) {
            uiFacade.getWindowManager().toggleMaximize();
        }
    }

    @FXML
    void onCloseWindow() {
        if (uiFacade != null && uiFacade.getWindowManager() != null) {
            uiFacade.getWindowManager().closeWindow();
        }
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
