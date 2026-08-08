package com.example.interfaz.service.ui;

import java.io.IOException;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.controller.LogsController;
import com.example.interfaz.service.LogService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class WindowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowManager.class);
    private final DialogService dialogService;
    private final WindowStageDecorator stageDecorator;
    private Stage primaryStage;

    public WindowManager(DialogService dialogService) {
        this.dialogService = dialogService;
        this.stageDecorator = new WindowStageDecorator();
    }

    public WindowManager() {
        this(new DialogService());
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public WindowStageDecorator getStageDecorator() {
        return stageDecorator;
    }

    public void initCustomStage(Stage stage, HBox customTitleBar, Node rootNode, FontIcon iconMaximize) {
        setPrimaryStage(stage);
        if (stageDecorator != null) {
            stageDecorator.attach(stage, customTitleBar, rootNode, iconMaximize);
        }
    }

    public void minimize() {
        if (stageDecorator != null) {
            stageDecorator.minimize();
        } else if (primaryStage != null) {
            primaryStage.setIconified(true);
        }
    }

    public void toggleMaximize() {
        if (stageDecorator != null) {
            stageDecorator.toggleMaximize();
        }
    }

    public void closeWindow() {
        if (stageDecorator != null) {
            stageDecorator.closeWindow();
        } else if (primaryStage != null) {
            primaryStage.fireEvent(new javafx.stage.WindowEvent(primaryStage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    public void showLogsWindow(Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/logs-view.fxml"));
            Parent root = loader.load();

            LogsController logsController = loader.getController();

            Stage logsStage = new Stage();
            logsStage.setTitle("📋 Logs de la Consola - YouTube Downloader");
            logsStage.initModality(Modality.NONE);
            if (ownerStage != null) {
                logsStage.initOwner(ownerStage);
            } else if (primaryStage != null) {
                logsStage.initOwner(primaryStage);
            }

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
            dialogService.showError("Error", "No se pudo abrir la ventana de logs: " + e.getMessage());
        }
    }
}

