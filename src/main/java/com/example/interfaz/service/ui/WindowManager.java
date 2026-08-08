package com.example.interfaz.service.ui;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.controller.LogsController;
import com.example.interfaz.service.LogService;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class WindowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowManager.class);
    private final DialogService dialogService;
    private Stage primaryStage;

    public WindowManager(DialogService dialogService) {
        this.dialogService = dialogService;
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
