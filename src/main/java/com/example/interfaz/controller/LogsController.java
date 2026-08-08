package com.example.interfaz.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.example.interfaz.service.LogService;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

@SuppressWarnings({"unused", "FXML"})
public class LogsController implements Initializable {

    @FXML
    private TextFlow logsTextFlow;

    @FXML
    private Button refreshButton;

    @FXML
    private Button clearLogsButton;

    @FXML
    private Button saveLogsButton;

    @FXML
    private Button closeButton;

    @FXML
    private CheckBox showExtractAudioCheckBox;

    @FXML
    private CheckBox showDownloadCheckBox;

    @FXML
    private CheckBox showOtherLogsCheckBox;

    @FXML
    private Button resetFiltersButton;

    private LogService logService;
    private Stage stage;
    private List<com.example.interfaz.model.LogEntry> allLogEntries;
    private Timeline autoRefreshTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logService = LogService.getInstance();
        setupLogsTextArea();
        loadLogs();

        startAutoRefresh();
    }

    private void setupLogsTextArea() {
    }

    private void loadLogs() {
        Platform.runLater(() -> {
            allLogEntries = logService.getLogEntries();
            applyFiltersAndColors();
        });
    }

    private void applyFiltersAndColors() {
        if (allLogEntries == null || logsTextFlow == null) {
            return;
        }

        List<com.example.interfaz.model.LogEntry> filteredLogs = allLogEntries.stream()
                .filter(this::shouldShowLog)
                .collect(Collectors.toList());

        logsTextFlow.getChildren().clear();

        for (com.example.interfaz.model.LogEntry entry : filteredLogs) {
            Text textNode = new Text(entry.getFormattedMessage() + "\n");

            switch (entry.getLevel()) {
                case ERROR ->
                    textNode.setFill(Color.web("#ff4d4d"));
                case WARNING ->
                    textNode.setFill(Color.web("#ffaa00"));
                case SYSTEM ->
                    textNode.setFill(Color.web("#bb86fc"));
                case DEBUG ->
                    textNode.setFill(Color.web("#a0a0b0"));
                case INFO -> {
                    String msg = entry.getMessage();
                    if (msg.contains("[ExtractAudio]")) {
                        textNode.setFill(Color.web("#00ff88"));
                    } else if (msg.contains("[download]")) {
                        textNode.setFill(Color.web("#4da6ff"));
                    } else {
                        textNode.setFill(Color.web("#e2e2e8"));
                    }
                }
            }

            logsTextFlow.getChildren().add(textNode);
        }
    }

    private boolean shouldShowLog(com.example.interfaz.model.LogEntry entry) {
        String msg = entry.getMessage();
        if (showExtractAudioCheckBox != null && showExtractAudioCheckBox.isSelected() && msg.contains("[ExtractAudio]")) {
            return true;
        }
        if (showDownloadCheckBox != null && showDownloadCheckBox.isSelected() && msg.contains("[download]")) {
            return true;
        }
        if (showOtherLogsCheckBox != null && showOtherLogsCheckBox.isSelected()) {
            return !msg.contains("[ExtractAudio]") && !msg.contains("[download]");
        }
        return false;
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> loadLogs()));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    @FXML
    private void onRefreshLogs() {
        loadLogs();
        showInfo("Logs actualizados", "Los logs se han actualizado correctamente.");
    }

    @FXML
    private void onClearLogs() {
        logService.clearLogs();
        logsTextFlow.getChildren().clear();
        allLogEntries = null;
        showInfo("Logs limpiados", "Todos los logs han sido eliminados.");
    }

    @FXML
    private void onFilterChanged() {
        applyFiltersAndColors();
    }

    @FXML
    private void onResetFilters() {
        showExtractAudioCheckBox.setSelected(true);
        showDownloadCheckBox.setSelected(true);
        showOtherLogsCheckBox.setSelected(true);
        applyFiltersAndColors();
    }

    @FXML
    private void onSaveLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Logs");
        fileChooser.setInitialFileName("logs_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");

        FileChooser.ExtensionFilter extFilter
                = new FileChooser.ExtensionFilter("Archivos de texto (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                StringBuilder content = new StringBuilder();
                logsTextFlow.getChildren().forEach(node -> {
                    if (node instanceof Text text) {
                        content.append(text.getText());
                    }
                });
                writer.write(content.toString());
                showInfo("Logs guardados", "Los logs se han guardado correctamente en: " + file.getAbsolutePath());
            } catch (IOException e) {
                showError("Error al guardar", "No se pudieron guardar los logs: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onClose() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        if (stage != null) {
            stage.close();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void showInfo(String title, String message) {
        com.example.interfaz.factory.ServiceFactory.getInstance().getDialogService().showInfo(title, message);
    }

    private void showError(String title, String message) {
        com.example.interfaz.factory.ServiceFactory.getInstance().getDialogService().showError(title, message);
    }
}
