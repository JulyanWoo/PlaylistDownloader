package com.example.interfaz.service.ui;

import java.io.File;

import com.example.interfaz.service.config.MusicFolderService;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public class FolderChooserService {

    private final MusicFolderService musicFolderService;
    private final DialogService dialogService;

    public FolderChooserService(MusicFolderService musicFolderService, DialogService dialogService) {
        this.musicFolderService = musicFolderService;
        this.dialogService = dialogService;
    }

    public FolderChooserService() {
        this(new MusicFolderService(), new DialogService());
    }

    public File selectFolder(Window owner, String title, File initialDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (title != null) {
            directoryChooser.setTitle(title);
        }
        if (initialDirectory != null && initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }
        return directoryChooser.showDialog(owner);
    }

    public String promptAndSelectFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta de Música");

        File currentDir = new File(musicFolderService.getCurrentMusicFolder());
        if (currentDir.exists()) {
            directoryChooser.setInitialDirectory(currentDir);
        }

        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            String newPath = musicFolderService.setMusicFolder(selectedDirectory.getAbsolutePath());
            dialogService.showInfo("Carpeta Actualizada", "La carpeta de música se ha cambiado a: " + newPath);
            return musicFolderService.getDisplayPath();
        }
        return musicFolderService.getDisplayPath();
    }

    public String resetToDefaultFolder() {
        String defaultPath = musicFolderService.resetToDefaultMusicFolder();
        dialogService.showInfo("Carpeta Restablecida", "La carpeta de música se ha restablecido a: " + defaultPath);
        return musicFolderService.getDisplayPath();
    }

    public String getDisplayPath() {
        return musicFolderService.getDisplayPath();
    }
}
