package com.example.interfaz.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.service.ui.analyzer.AnalyzerRowModel;
import com.example.interfaz.service.ui.analyzer.LanguageSongRowModel;
import com.example.interfaz.service.ui.analyzer.LibraryAnalyzerPresenter;
import com.example.interfaz.service.ui.analyzer.LibraryAnalyzerViewControls;
import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import javafx.fxml.FXML;
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

@SuppressWarnings({"unused", "FXML"})
public class LibraryAnalyzerController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryAnalyzerController.class);

    @FXML private Button btnStartAnalysis;
    @FXML private Button btnCancelAnalysis;
    @FXML private ComboBox<LanguageDetectorMode> cmbLanguageMode;
    @FXML private VBox progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Label duplicatesFoundLabel;
    @FXML private Label timeRemainingLabel;

    @FXML private HBox statsBox;
    @FXML private Label lblStatTotalSongs;
    @FXML private Label lblStatTotalFiles;
    @FXML private Label lblStatGroups;
    @FXML private Label lblStatRecoverableSpace;
    @FXML private Label lblStatDuration;

    @FXML private TitledPane duplicatesPane;
    @FXML private TreeTableView<AnalyzerRowModel> resultsTreeTable;
    @FXML private TreeTableColumn<AnalyzerRowModel, Boolean> colSelect;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colName;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colArtist;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colDuration;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colSize;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colType;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colLanguage;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colStatus;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colPath;

    @FXML private Label selectedSummaryLabel;
    @FXML private Button btnKeepOriginal;
    @FXML private Button btnSelectDuplicates;
    @FXML private Button btnUnselectAll;
    @FXML private Button btnMoveToTrash;

    @FXML private TitledPane languageBrowserPane;
    @FXML private ComboBox<String> cmbLangFilter;
    @FXML private TextField txtLangSearch;
    @FXML private Label lblLangCount;
    @FXML private TableView<LanguageSongRowModel> langBrowserTable;
    @FXML private TableColumn<LanguageSongRowModel, Boolean> colLangSelect;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangName;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangArtist;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangLang;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangConf;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangMethod;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangFormat;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangSize;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangPath;
    @FXML private Label lblLangBrowserSummary;
    @FXML private Button btnLangSelectAll;
    @FXML private Button btnLangDeselectAll;
    @FXML private Button btnLangQuarantine;

    private final LibraryAnalyzerViewModel viewModel = new LibraryAnalyzerViewModel();
    private LibraryAnalyzerPresenter presenter;

    @FXML
    void initialize() {
        this.presenter = ServiceFactory.getInstance().getLibraryAnalyzerPresenter(viewModel);
        this.presenter.initialize(createControlsHolder());
        LOGGER.info("LibraryAnalyzerController inicializado correctamente");
    }

    private LibraryAnalyzerViewControls createControlsHolder() {
        return new LibraryAnalyzerViewControls(
                btnStartAnalysis, btnCancelAnalysis, cmbLanguageMode,
                progressBox, progressBar, statusLabel, duplicatesFoundLabel, timeRemainingLabel,
                statsBox, lblStatTotalSongs, lblStatTotalFiles, lblStatGroups, lblStatRecoverableSpace, lblStatDuration,
                selectedSummaryLabel, lblLangBrowserSummary,
                duplicatesPane, resultsTreeTable,
                colSelect, colName, colArtist, colDuration, colSize, colType, colLanguage, colStatus, colPath,
                languageBrowserPane, cmbLangFilter, txtLangSearch, lblLangCount,
                langBrowserTable, colLangSelect, colLangName, colLangArtist, colLangLang,
                colLangConf, colLangMethod, colLangFormat, colLangSize, colLangPath
        );
    }

    @FXML void onStartAnalysis() { presenter.startAnalysis(); }
    @FXML void onCancelAnalysis() { presenter.cancelAnalysis(); }
    @FXML void onKeepOriginal() { presenter.keepOriginals(); }
    @FXML void onSelectDuplicates() { presenter.selectDuplicates(); }
    @FXML void onUnselectAll() { presenter.unselectAll(); }
    @FXML void onMoveToTrash() { presenter.moveSelectedToTrash(); }
    @FXML void onLangSelectAll() { presenter.selectAllLanguages(); }
    @FXML void onLangDeselectAll() { presenter.deselectAllLanguages(); }
    @FXML void onLangQuarantine() { presenter.quarantineSelectedLanguages(); }

    @Override
    public void close() {
        if (presenter != null) {
            presenter.close();
        }
        LOGGER.info("LibraryAnalyzerController cerrado.");
    }
}
