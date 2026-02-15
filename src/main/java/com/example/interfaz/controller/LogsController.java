package com.example.interfaz.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.example.interfaz.service.LogService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    private List<String> allLogLines;

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
            String logs = logService.getAllLogs();
            allLogLines = Arrays.asList(logs.split("\n"));
            applyFiltersAndColors();
        });
    }

    private void applyFiltersAndColors() {
        if (allLogLines == null) return;
        
        List<String> filteredLogs = allLogLines.stream()
            .filter(this::shouldShowLog)
            .collect(Collectors.toList());
        
        logsTextFlow.getChildren().clear();
        
        for (String line : filteredLogs) {
            Text textNode = new Text(line + "\n");
            
            if (line.contains("[ExtractAudio]")) {
                textNode.setFill(Color.web("#28a745"));
            } else if (line.contains("[download]")) {
                textNode.setFill(Color.web("#dc3545")); 
            } else {
                textNode.setFill(Color.web("#ffffff"));
            }
            
            logsTextFlow.getChildren().add(textNode);
        }
    }

    private boolean shouldShowLog(String logLine) {
        if (logLine.contains("[ExtractAudio]") && showExtractAudioCheckBox.isSelected()) {
            return true;
        }
        if (logLine.contains("[download]") && showDownloadCheckBox.isSelected()) {
            return true;
        }
        if (!logLine.contains("[ExtractAudio]") && !logLine.contains("[download]") && showOtherLogsCheckBox.isSelected()) {
            return true;
        }
        return false;
    }

    private void startAutoRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                    if (stage != null && stage.isShowing()) {
                        loadLogs();
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
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
        allLogLines = null;
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
        fileChooser.setInitialFileName("logs_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
        
        FileChooser.ExtensionFilter extFilter = 
            new FileChooser.ExtensionFilter("Archivos de texto (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                StringBuilder content = new StringBuilder();
                logsTextFlow.getChildren().forEach(node -> {
                    if (node instanceof Text) {
                        content.append(((Text) node).getText());
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
        if (stage != null) {
            stage.close();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}