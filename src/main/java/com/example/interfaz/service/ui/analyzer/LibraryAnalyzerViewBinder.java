package com.example.interfaz.service.ui.analyzer;

import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LibraryAnalyzerViewBinder {

    public void bind(
            LibraryAnalyzerViewModel viewModel,
            Button btnStartAnalysis,
            Button btnCancelAnalysis,
            ComboBox<LanguageDetectorMode> cmbLanguageMode,
            VBox progressBox,
            ProgressBar progressBar,
            Label statusLabel,
            Label duplicatesFoundLabel,
            Label timeRemainingLabel,
            HBox statsBox,
            Label lblStatTotalSongs,
            Label lblStatTotalFiles,
            Label lblStatGroups,
            Label lblStatRecoverableSpace,
            Label lblStatDuration,
            Label selectedSummaryLabel,
            Label lblLangBrowserSummary
    ) {
        if (viewModel == null) return;

        if (btnStartAnalysis != null) {
            btnStartAnalysis.disableProperty().bind(viewModel.analyzingProperty());
        }
        if (btnCancelAnalysis != null) {
            btnCancelAnalysis.disableProperty().bind(viewModel.analyzingProperty().not());
        }

        if (cmbLanguageMode != null) {
            cmbLanguageMode.getItems().setAll(LanguageDetectorMode.values());
            cmbLanguageMode.valueProperty().bindBidirectional(viewModel.languageDetectorModeProperty());
            cmbLanguageMode.disableProperty().bind(viewModel.analyzingProperty());
        }

        if (progressBox != null) {
            progressBox.visibleProperty().bind(viewModel.analyzingProperty());
            progressBox.managedProperty().bind(viewModel.analyzingProperty());
        }

        if (progressBar != null) {
            progressBar.progressProperty().bind(viewModel.progressProperty());
        }
        if (statusLabel != null) {
            statusLabel.textProperty().bind(viewModel.statusTextProperty());
        }
        if (duplicatesFoundLabel != null) {
            duplicatesFoundLabel.textProperty().bind(viewModel.duplicatesFoundTextProperty());
        }
        if (timeRemainingLabel != null) {
            timeRemainingLabel.textProperty().bind(viewModel.timeRemainingTextProperty());
        }

        if (statsBox != null) {
            statsBox.visibleProperty().bind(viewModel.statsVisibleProperty());
            statsBox.managedProperty().bind(viewModel.statsVisibleProperty());
        }

        if (lblStatTotalSongs != null) {
            lblStatTotalSongs.textProperty().bind(viewModel.statTotalSongsTextProperty());
        }
        if (lblStatTotalFiles != null) {
            lblStatTotalFiles.textProperty().bind(viewModel.statTotalFilesTextProperty());
        }
        if (lblStatGroups != null) {
            lblStatGroups.textProperty().bind(viewModel.statGroupsTextProperty());
        }
        if (lblStatRecoverableSpace != null) {
            lblStatRecoverableSpace.textProperty().bind(viewModel.statRecoverableSpaceTextProperty());
        }
        if (lblStatDuration != null) {
            lblStatDuration.textProperty().bind(viewModel.statDurationTextProperty());
        }

        if (selectedSummaryLabel != null) {
            selectedSummaryLabel.textProperty().bind(viewModel.selectedSummaryTextProperty());
        }
        if (lblLangBrowserSummary != null) {
            lblLangBrowserSummary.textProperty().bind(viewModel.langBrowserSummaryTextProperty());
        }
    }
}
