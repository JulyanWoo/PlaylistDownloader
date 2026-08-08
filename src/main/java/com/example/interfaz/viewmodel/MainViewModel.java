package com.example.interfaz.viewmodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.service.download.YtDlpUpdateService;
import com.example.interfaz.service.ui.DialogService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MainViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewModel.class);

    private final StringProperty musicFolderDisplayPath = new SimpleStringProperty("");
    private final StringProperty ytDlpVersionText = new SimpleStringProperty("Ver: --");
    private final StringProperty ytDlpStatusText = new SimpleStringProperty("Estado: --");
    private final StringProperty updateButtonText = new SimpleStringProperty("Buscar actualización");
    private final BooleanProperty updateButtonDisabled = new SimpleBooleanProperty(false);

    public StringProperty musicFolderDisplayPathProperty() { return musicFolderDisplayPath; }
    public StringProperty ytDlpVersionTextProperty() { return ytDlpVersionText; }
    public StringProperty ytDlpStatusTextProperty() { return ytDlpStatusText; }
    public StringProperty updateButtonTextProperty() { return updateButtonText; }
    public BooleanProperty updateButtonDisabledProperty() { return updateButtonDisabled; }

    public void updateMusicFolderDisplay(String path) {
        if (path == null || path.trim().isEmpty()) {
            musicFolderDisplayPath.set("Ruta no configurada");
        } else {
            musicFolderDisplayPath.set(path);
        }
    }

    public void checkAndPerformYtDlpUpdate(YtDlpUpdateService ytDlpUpdateService, DialogService dialogService) {
        if (ytDlpUpdateService == null) return;

        updateButtonDisabled.set(true);
        updateButtonText.set("Buscando...");
        ytDlpStatusText.set("Estado: Verificando...");

        ytDlpUpdateService.checkUpdateAsync(true).thenAccept(info -> {
            Platform.runLater(() -> {
                if (info != null && info.isUpdateAvailable()) {
                    ytDlpStatusText.set("Estado: Descargando...");
                    updateButtonText.set("Descargando...");

                    ytDlpUpdateService.updateYtDlpAsync(log -> LOGGER.info("[yt-dlp update log] {}", log))
                            .thenAccept(success -> Platform.runLater(() -> {
                                updateButtonDisabled.set(false);
                                updateButtonText.set("Buscar actualización");
                                if (success) {
                                    ytDlpStatusText.set("Estado: Actualizado");
                                    ytDlpVersionText.set("Ver: " + (info.getLatestVersion() != null ? info.getLatestVersion() : "--"));
                                    if (dialogService != null) {
                                        dialogService.showInfo("yt-dlp Actualizado", "La herramienta yt-dlp ha sido actualizada correctamente.");
                                    }
                                } else {
                                    ytDlpStatusText.set("Estado: Error al actualizar");
                                    if (dialogService != null) {
                                        dialogService.showError("Error de Actualización", "No se pudo actualizar yt-dlp.");
                                    }
                                }
                            }));
                } else {
                    ytDlpStatusText.set("Estado: Al día");
                    if (info != null && info.getCurrentVersion() != null) {
                        ytDlpVersionText.set("Ver: " + info.getCurrentVersion());
                    }
                    updateButtonText.set("Buscar actualización");
                    updateButtonDisabled.set(false);
                    if (dialogService != null) {
                        dialogService.showInfo("yt-dlp Al día", "Ya tienes la última versión de yt-dlp instalada.");
                    }
                }
            });
        }).exceptionally(ex -> {
            LOGGER.error("Error comprobando actualización de yt-dlp", ex);
            Platform.runLater(() -> {
                ytDlpStatusText.set("Estado: Error");
                updateButtonText.set("Reintentar");
                updateButtonDisabled.set(false);
            });
            return null;
        });
    }
}
