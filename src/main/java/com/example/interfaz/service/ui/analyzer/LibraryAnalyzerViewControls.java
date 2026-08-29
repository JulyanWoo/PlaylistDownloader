package com.example.interfaz.service.ui.analyzer;

import com.example.interfaz.model.analyzer.LanguageDetectorMode;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public record LibraryAnalyzerViewControls(
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
        Label lblLangBrowserSummary,
        TitledPane duplicatesPane,
        TreeTableView<AnalyzerRowModel> resultsTreeTable,
        TreeTableColumn<AnalyzerRowModel, Boolean> colSelect,
        TreeTableColumn<AnalyzerRowModel, String> colName,
        TreeTableColumn<AnalyzerRowModel, String> colArtist,
        TreeTableColumn<AnalyzerRowModel, String> colDuration,
        TreeTableColumn<AnalyzerRowModel, String> colSize,
        TreeTableColumn<AnalyzerRowModel, String> colType,
        TreeTableColumn<AnalyzerRowModel, String> colLanguage,
        TreeTableColumn<AnalyzerRowModel, String> colStatus,
        TreeTableColumn<AnalyzerRowModel, String> colPath,
        TitledPane languageBrowserPane,
        ComboBox<String> cmbLangFilter,
        TextField txtLangSearch,
        Label lblLangCount,
        TableView<LanguageSongRowModel> langBrowserTable,
        TableColumn<LanguageSongRowModel, Boolean> colLangSelect,
        TableColumn<LanguageSongRowModel, String> colLangName,
        TableColumn<LanguageSongRowModel, String> colLangArtist,
        TableColumn<LanguageSongRowModel, String> colLangLang,
        TableColumn<LanguageSongRowModel, String> colLangConf,
        TableColumn<LanguageSongRowModel, String> colLangMethod,
        TableColumn<LanguageSongRowModel, String> colLangFormat,
        TableColumn<LanguageSongRowModel, String> colLangSize,
        TableColumn<LanguageSongRowModel, String> colLangPath
) {}
