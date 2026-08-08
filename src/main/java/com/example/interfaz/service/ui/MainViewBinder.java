package com.example.interfaz.service.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.service.config.MusicFolderService;
import com.example.interfaz.viewmodel.MainViewModel;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainViewBinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewBinder.class);

    public void bindFolderManager(MainViewModel mainViewModel, Label musicFolderLabel, MusicFolderService musicFolderService) {
        if (mainViewModel == null || musicFolderService == null) {
            return;
        }

        mainViewModel.musicFolderDisplayPathProperty().addListener((obs, oldVal, newVal) -> {
            if (musicFolderLabel != null) {
                musicFolderLabel.setText(newVal);
            }
        });

        mainViewModel.updateMusicFolderDisplay(musicFolderService.getMusicFolderPath());
        LOGGER.info("MainViewBinder: FolderManager vinculado correctamente.");
    }

    public void bindYtDlpUpdater(
            MainViewModel mainViewModel,
            Label ytDlpVersionLabel,
            Label ytDlpStatusLabel,
            Button updateYtDlpButton
    ) {
        if (mainViewModel == null) {
            return;
        }

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
        LOGGER.info("MainViewBinder: YtDlpUpdater vinculado correctamente.");
    }
}
