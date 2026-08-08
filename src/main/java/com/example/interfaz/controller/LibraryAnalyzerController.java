package com.example.interfaz.controller;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisCancelled;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisFinished;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisStarted;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryProgressUpdated;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.DuplicateManagementService;
import com.example.interfaz.service.analyzer.DuplicateSelectionService;
import com.example.interfaz.service.analyzer.LibraryAnalyzerService;
import com.example.interfaz.service.analyzer.SongLanguageBrowserService;
import com.example.interfaz.service.ui.DialogService;
import com.example.interfaz.service.ui.analyzer.AnalyzerRowModel;
import com.example.interfaz.service.ui.analyzer.AnalyzerTableConfigurator;
import com.example.interfaz.service.ui.analyzer.LanguageSongRowModel;
import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@SuppressWarnings({"unused", "FXML"})
public class LibraryAnalyzerController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryAnalyzerController.class);

    // ── Analysis controls ─────────────────────────────────────────────────────
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

    // ── Duplicates pane ───────────────────────────────────────────────────────
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

    // ── Language Browser pane ─────────────────────────────────────────────────
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
    @FXML private TableColumn<LanguageSongRowModel, String> colLangFormat;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangSize;
    @FXML private TableColumn<LanguageSongRowModel, String> colLangPath;
    @FXML private Label lblLangBrowserSummary;
    @FXML private Button btnLangSelectAll;
    @FXML private Button btnLangDeselectAll;
    @FXML private Button btnLangQuarantine;

    // ── Services ──────────────────────────────────────────────────────────────
    private LibraryAnalyzerService libraryAnalyzerService;
    private DuplicateSelectionService duplicateSelectionService;
    private DuplicateManagementService duplicateManagementService;
    private SongLanguageBrowserService songLanguageBrowserService;
    private AnalyzerTableConfigurator tableConfigurator;
    private DialogService dialogService;
    private EventPublisher eventPublisher;

    private final LibraryAnalyzerViewModel viewModel = new LibraryAnalyzerViewModel();

    private final Consumer<LibraryAnalysisStarted> startedListener = this::onAnalysisStartedEvent;
    private final Consumer<LibraryProgressUpdated> progressListener = this::onProgressUpdatedEvent;
    private final Consumer<LibraryAnalysisFinished> finishedListener = this::onAnalysisFinishedEvent;
    private final Consumer<LibraryAnalysisCancelled> cancelledListener = this::onAnalysisCancelledEvent;

    @FXML
    void initialize() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.libraryAnalyzerService = factory.getLibraryAnalyzerService();
        this.duplicateSelectionService = factory.getDuplicateSelectionService();
        this.duplicateManagementService = factory.getDuplicateManagementService();
        this.songLanguageBrowserService = factory.getSongLanguageBrowserService();
        this.tableConfigurator = factory.getAnalyzerTableConfigurator();
        this.dialogService = factory.getDialogService();
        this.eventPublisher = factory.getEventPublisher();

        setupBindings();
        setupTable();
        setupLanguageBrowser();
        setupGroupListeners();
        registerEventSubscriptions();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupBindings() {
        btnStartAnalysis.disableProperty().bind(viewModel.analyzingProperty());
        btnCancelAnalysis.disableProperty().bind(viewModel.analyzingProperty().not());

        if (cmbLanguageMode != null) {
            cmbLanguageMode.getItems().setAll(LanguageDetectorMode.values());
            cmbLanguageMode.valueProperty().bindBidirectional(viewModel.languageDetectorModeProperty());
            cmbLanguageMode.disableProperty().bind(viewModel.analyzingProperty());
        }

        progressBox.visibleProperty().bind(viewModel.analyzingProperty());
        progressBox.managedProperty().bind(viewModel.analyzingProperty());

        progressBar.progressProperty().bind(viewModel.progressProperty());
        statusLabel.textProperty().bind(viewModel.statusTextProperty());
        duplicatesFoundLabel.textProperty().bind(viewModel.duplicatesFoundTextProperty());
        timeRemainingLabel.textProperty().bind(viewModel.timeRemainingTextProperty());

        statsBox.visibleProperty().bind(viewModel.statsVisibleProperty());
        statsBox.managedProperty().bind(viewModel.statsVisibleProperty());

        lblStatTotalSongs.textProperty().bind(viewModel.statTotalSongsTextProperty());
        lblStatTotalFiles.textProperty().bind(viewModel.statTotalFilesTextProperty());
        lblStatGroups.textProperty().bind(viewModel.statGroupsTextProperty());
        lblStatRecoverableSpace.textProperty().bind(viewModel.statRecoverableSpaceTextProperty());
        lblStatDuration.textProperty().bind(viewModel.statDurationTextProperty());

        selectedSummaryLabel.textProperty().bind(viewModel.selectedSummaryTextProperty());
        lblLangBrowserSummary.textProperty().bind(viewModel.langBrowserSummaryTextProperty());
    }

    private void setupTable() {
        tableConfigurator.configure(
                resultsTreeTable,
                colSelect, colName, colArtist, colDuration, colSize, colType, colLanguage, colStatus, colPath,
                this::updateSelectedSummary
        );
    }

    private void setupLanguageBrowser() {
        // Populate language filter ComboBox
        cmbLangFilter.getItems().setAll(
                "Todos", "Español", "Inglés", "Portugués", "Francés",
                "Asiático (CJK)", "Mixto / Bilingüe", "Desconocido"
        );
        cmbLangFilter.getSelectionModel().select("Todos");

        // Bind ComboBox & search field to ViewModel
        cmbLangFilter.valueProperty().addListener((obs, old, nv) -> {
            viewModel.langFilterProperty().set(nv != null ? nv : "Todos");
            refreshLangCount();
        });

        txtLangSearch.textProperty().bindBidirectional(viewModel.langSearchProperty());
        txtLangSearch.textProperty().addListener((obs, old, nv) -> refreshLangCount());

        // Configure TableView columns
        langBrowserTable.setEditable(true);

        colLangSelect.setCellValueFactory(param -> {
            LanguageSongRowModel model = param.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(model.isSelected());
            prop.addListener((obs, old, nv) -> {
                model.setSelected(nv);
                viewModel.updateLangBrowserSummary();
            });
            return prop;
        });
        colLangSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colLangSelect));
        colLangSelect.setEditable(true);

        colLangName.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getName() : ""));

        colLangArtist.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getArtist() : ""));

        colLangLang.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getLanguage() : ""));
        // Language cell factory with color coding
        colLangLang.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(getLangStyle(item));
                }
            }
        });

        colLangConf.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getConfidence() : ""));

        colLangFormat.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getFormat() : ""));

        colLangSize.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getSizeFormatted() : ""));

        colLangPath.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null ? param.getValue().getPath() : ""));

        // Bind table to filtered list from ViewModel
        langBrowserTable.setItems(viewModel.getFilteredSongModels());

        // Update count label when filtered list changes
        viewModel.getFilteredSongModels().addListener((javafx.collections.ListChangeListener<LanguageSongRowModel>) c -> refreshLangCount());
    }

    /** Returns an inline CSS style based on the detected language name. */
    private String getLangStyle(String langName) {
        if (langName == null) return "";
        return switch (langName) {
            case "Español" -> "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
            case "Inglés" -> "-fx-text-fill: #2196F3; -fx-font-weight: bold;";
            case "Portugués" -> "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
            case "Francés" -> "-fx-text-fill: #9C27B0; -fx-font-weight: bold;";
            case "Asiático (CJK)" -> "-fx-text-fill: #F44336; -fx-font-weight: bold;";
            case "Mixto / Bilingüe" -> "-fx-text-fill: #795548; -fx-font-weight: bold;";
            default -> "-fx-text-fill: -color-fg-muted;";
        };
    }

    private void refreshLangCount() {
        int count = viewModel.getFilteredSongModels().size();
        lblLangCount.setText(count + " canción" + (count == 1 ? "" : "es"));
    }

    private void setupGroupListeners() {
        viewModel.getCurrentGroups().addListener((ListChangeListener<DuplicateGroup>) change -> refreshTable());
    }

    private void registerEventSubscriptions() {
        if (eventPublisher != null) {
            eventPublisher.subscribe(LibraryAnalysisStarted.class, startedListener);
            eventPublisher.subscribe(LibraryProgressUpdated.class, progressListener);
            eventPublisher.subscribe(LibraryAnalysisFinished.class, finishedListener);
            eventPublisher.subscribe(LibraryAnalysisCancelled.class, cancelledListener);
        }
    }

    private void unregisterEventSubscriptions() {
        if (eventPublisher != null) {
            eventPublisher.unsubscribe(LibraryAnalysisStarted.class, startedListener);
            eventPublisher.unsubscribe(LibraryProgressUpdated.class, progressListener);
            eventPublisher.unsubscribe(LibraryAnalysisFinished.class, finishedListener);
            eventPublisher.unsubscribe(LibraryAnalysisCancelled.class, cancelledListener);
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void onAnalysisStartedEvent(LibraryAnalysisStarted event) {
        safeUpdate(() -> viewModel.startAnalysis(event.getFolderPath()));
    }

    private void onProgressUpdatedEvent(LibraryProgressUpdated event) {
        safeUpdate(() -> viewModel.updateProgress(
                event.getProcessedSongs(),
                event.getTotalFiles(),
                event.getDuplicatesFound(),
                event.getEstimatedRemainingMillis()
        ));
    }

    private void onAnalysisFinishedEvent(LibraryAnalysisFinished event) {
        safeUpdate(() -> {
            viewModel.finishAnalysis(event.getResult());
            updateSelectedSummary();
            refreshLangCount();
            // Auto-expand Language Browser pane if songs were detected
            if (event.getResult().getAllSongs() != null && !event.getResult().getAllSongs().isEmpty()) {
                languageBrowserPane.setExpanded(true);
            }
        });
    }

    private void onAnalysisCancelledEvent(LibraryAnalysisCancelled event) {
        safeUpdate(() -> {
            viewModel.cancelAnalysis(event.getPartialResult());
            updateSelectedSummary();
            refreshLangCount();
            if (dialogService != null) {
                dialogService.showInfo("Análisis cancelado", "El análisis fue cancelado por el usuario. Se muestran los resultados parciales.");
            }
        });
    }

    private void refreshTable() {
        tableConfigurator.populate(resultsTreeTable, viewModel.getCurrentGroups());
        // Update duplicate pane title with count
        if (duplicatesPane != null) {
            int count = viewModel.getCurrentGroups().size();
            duplicatesPane.setText("  Canciones Duplicadas" + (count > 0 ? " (" + count + " grupos)" : ""));
        }
    }

    private void updateSelectedSummary() {
        var summary = duplicateSelectionService.calculateSelectionSummary(viewModel.getCurrentGroups());
        viewModel.selectedSummaryTextProperty().set(summary.getFormattedSummary());
    }

    // ── Duplicate pane actions ────────────────────────────────────────────────

    @FXML
    void onStartAnalysis() {
        if (viewModel.analyzingProperty().get()) return;
        if (libraryAnalyzerService != null) {
            try {
                libraryAnalyzerService.setLanguageDetectorMode(viewModel.getLanguageDetectorMode());
                libraryAnalyzerService.startAnalysis();
            } catch (Exception e) {
                LOGGER.error("Error al iniciar el análisis de biblioteca", e);
                if (dialogService != null) {
                    dialogService.showError("Error de análisis", "No se pudo iniciar el análisis: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    void onCancelAnalysis() {
        if (libraryAnalyzerService != null) {
            libraryAnalyzerService.cancelAnalysis();
        }
    }

    @FXML
    void onKeepOriginal() {
        duplicateSelectionService.keepOriginals(viewModel.getCurrentGroups());
        refreshTable();
        updateSelectedSummary();
    }

    @FXML
    void onSelectDuplicates() {
        duplicateSelectionService.selectDuplicates(viewModel.getCurrentGroups());
        refreshTable();
        updateSelectedSummary();
    }

    @FXML
    void onUnselectAll() {
        duplicateSelectionService.unselectAll(viewModel.getCurrentGroups());
        refreshTable();
        updateSelectedSummary();
    }

    @FXML
    void onMoveToTrash() {
        List<DuplicateCandidate> selected = duplicateSelectionService.getSelectedCandidates(viewModel.getCurrentGroups());
        if (selected.isEmpty()) {
            if (dialogService != null) {
                dialogService.showInfo("Sin selección", "No hay archivos marcados para mover a cuarentena.");
            }
            return;
        }

        var result = duplicateManagementService.moveSelectedToQuarantine(selected, viewModel.getCurrentGroups());
        if (dialogService != null) {
            dialogService.showInfo("Archivos movidos", String.format(
                    "Se movieron correctamente %d archivos a la carpeta de cuarentena (.duplicates).", result.countMoved()));
        }

        viewModel.updateGroups(result.remainingGroups());
        updateSelectedSummary();
    }

    // ── Language Browser actions ──────────────────────────────────────────────

    @FXML
    void onLangSelectAll() {
        viewModel.selectAllVisible();
    }

    @FXML
    void onLangDeselectAll() {
        viewModel.deselectAllSongs();
    }

    @FXML
    void onLangQuarantine() {
        List<SongFile> selected = viewModel.getSelectedSongsForQuarantine();
        if (selected.isEmpty()) {
            if (dialogService != null) {
                dialogService.showInfo("Sin selección", "No hay canciones seleccionadas para mover a cuarentena.");
            }
            return;
        }

        if (songLanguageBrowserService == null) {
            LOGGER.error("SongLanguageBrowserService not initialized");
            return;
        }

        SongLanguageBrowserService.QuarantineResult result = songLanguageBrowserService.moveToQuarantine(selected);

        if (result.moved() > 0) {
            viewModel.removeSongsFromBrowser(selected);
            refreshLangCount();
        }

        if (dialogService != null) {
            if (result.failed() == 0) {
                dialogService.showInfo("Archivos movidos",
                        String.format("Se movieron %d canción(es) a la carpeta '.quarantine-idioma'.", result.moved()));
            } else {
                dialogService.showInfo("Operación parcial",
                        String.format("Se movieron %d canción(es). %d no pudieron moverse.", result.moved(), result.failed()));
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void safeUpdate(Runnable action) {
        Runnable wrapped = () -> {
            try {
                action.run();
            } catch (Exception e) {
                LOGGER.error("Error actualizando LibraryAnalyzer UI", e);
            }
        };

        if (Platform.isFxApplicationThread()) {
            wrapped.run();
        } else {
            Platform.runLater(wrapped);
        }
    }

    @Override
    public void close() {
        unregisterEventSubscriptions();
        LOGGER.info("LibraryAnalyzerController cerrado y suscripciones desvinculadas.");
    }
}
