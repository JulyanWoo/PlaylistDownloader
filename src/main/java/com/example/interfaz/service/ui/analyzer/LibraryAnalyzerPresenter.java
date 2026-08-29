package com.example.interfaz.service.ui.analyzer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisCancelled;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisFinished;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisStarted;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryProgressUpdated;
import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.LibraryAnalyzerFacade;
import com.example.interfaz.service.ui.DialogService;
import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class LibraryAnalyzerPresenter implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryAnalyzerPresenter.class);

    private final LibraryAnalyzerFacade facade;
    private final LibraryAnalyzerViewModel viewModel;
    private final LibraryAnalyzerViewBinder viewBinder;
    private final AnalyzerTableConfigurator tableConfigurator;
    private final LanguageBrowserTableConfigurator languageConfigurator;
    private final DialogService dialogService;

    private TreeTableView<AnalyzerRowModel> resultsTreeTable;
    private TitledPane duplicatesPane;
    private TitledPane languageBrowserPane;
    private Label lblLangCount;

    public LibraryAnalyzerPresenter(
            LibraryAnalyzerFacade facade,
            LibraryAnalyzerViewModel viewModel,
            LibraryAnalyzerViewBinder viewBinder,
            AnalyzerTableConfigurator tableConfigurator,
            LanguageBrowserTableConfigurator languageConfigurator,
            DialogService dialogService
    ) {
        this.facade = facade;
        this.viewModel = viewModel;
        this.viewBinder = viewBinder;
        this.tableConfigurator = tableConfigurator;
        this.languageConfigurator = languageConfigurator;
        this.dialogService = dialogService;
    }

    public void initialize(LibraryAnalyzerViewControls controls) {
        if (controls == null) return;

        this.duplicatesPane = controls.duplicatesPane();
        this.resultsTreeTable = controls.resultsTreeTable();
        this.languageBrowserPane = controls.languageBrowserPane();
        this.lblLangCount = controls.lblLangCount();

        if (duplicatesPane != null) {
            duplicatesPane.expandedProperty().addListener((obs, oldVal, isExpanded) -> {
                VBox.setVgrow(duplicatesPane, isExpanded ? Priority.ALWAYS : Priority.NEVER);
            });
            VBox.setVgrow(duplicatesPane, duplicatesPane.isExpanded() ? Priority.ALWAYS : Priority.NEVER);
        }

        if (languageBrowserPane != null) {
            languageBrowserPane.expandedProperty().addListener((obs, oldVal, isExpanded) -> {
                VBox.setVgrow(languageBrowserPane, isExpanded ? Priority.ALWAYS : Priority.NEVER);
            });
            VBox.setVgrow(languageBrowserPane, languageBrowserPane.isExpanded() ? Priority.ALWAYS : Priority.NEVER);
        }

        if (viewBinder != null) {
            viewBinder.bind(
                    viewModel,
                    controls.btnStartAnalysis(), controls.btnCancelAnalysis(), controls.cmbLanguageMode(),
                    controls.progressBox(), controls.progressBar(), controls.statusLabel(), controls.duplicatesFoundLabel(), controls.timeRemainingLabel(),
                    controls.statsBox(), controls.lblStatTotalSongs(), controls.lblStatTotalFiles(), controls.lblStatGroups(), controls.lblStatRecoverableSpace(), controls.lblStatDuration(),
                    controls.selectedSummaryLabel(), controls.lblLangBrowserSummary()
            );
        }

        if (tableConfigurator != null) {
            tableConfigurator.configure(
                    controls.resultsTreeTable(),
                    controls.colSelect(), controls.colName(), controls.colArtist(), controls.colDuration(), controls.colSize(), controls.colType(), controls.colLanguage(), controls.colStatus(), controls.colPath(),
                    this::updateSelectedSummary
            );
        }

        if (languageConfigurator != null) {
            languageConfigurator.configure(
                    controls.langBrowserTable(),
                    controls.colLangSelect(), controls.colLangName(), controls.colLangArtist(),
                    controls.colLangLang(), controls.colLangConf(), controls.colLangMethod(),
                    controls.colLangFormat(), controls.colLangSize(), controls.colLangPath(),
                    controls.cmbLangFilter(), controls.txtLangSearch(), controls.lblLangCount(), viewModel
            );
        }

        if (viewModel != null) {
            viewModel.getCurrentGroups().addListener((ListChangeListener<DuplicateGroup>) change -> refreshTable());
        }

        registerEventSubscriptions();
    }

    private void registerEventSubscriptions() {
        if (facade != null) {
            facade.subscribeToEvents(
                    this::onAnalysisStartedEvent,
                    this::onProgressUpdatedEvent,
                    this::onAnalysisFinishedEvent,
                    this::onAnalysisCancelledEvent
            );
        }
    }

    private void onAnalysisStartedEvent(LibraryAnalysisStarted event) {
        safeUpdate(() -> {
            if (viewModel != null) viewModel.startAnalysis(event.getFolderPath());
        });
    }

    private void onProgressUpdatedEvent(LibraryProgressUpdated event) {
        safeUpdate(() -> {
            if (viewModel != null) viewModel.updateProgress(
                    event.getProcessedSongs(),
                    event.getTotalFiles(),
                    event.getDuplicatesFound(),
                    event.getEstimatedRemainingMillis()
            );
        });
    }

    private void onAnalysisFinishedEvent(LibraryAnalysisFinished event) {
        safeUpdate(() -> {
            if (viewModel != null) viewModel.finishAnalysis(event.getResult());
            updateSelectedSummary();
            refreshLangCount();
            if (languageBrowserPane != null && event.getResult() != null && event.getResult().getAllSongs() != null && !event.getResult().getAllSongs().isEmpty()) {
                languageBrowserPane.setExpanded(true);
            }
        });
    }

    private void onAnalysisCancelledEvent(LibraryAnalysisCancelled event) {
        safeUpdate(() -> {
            if (viewModel != null) viewModel.cancelAnalysis(event.getPartialResult());
            updateSelectedSummary();
            refreshLangCount();
            if (dialogService != null) {
                dialogService.showInfo("Análisis cancelado", "El análisis fue cancelado por el usuario. Se muestran los resultados parciales.");
            }
        });
    }

    public void refreshTable() {
        if (tableConfigurator != null && resultsTreeTable != null && viewModel != null) {
            tableConfigurator.populate(resultsTreeTable, viewModel.getCurrentGroups());
        }
        if (duplicatesPane != null && viewModel != null) {
            int count = viewModel.getCurrentGroups().size();
            duplicatesPane.setText("  Canciones Duplicadas" + (count > 0 ? " (" + count + " grupos)" : ""));
        }
    }

    public void refreshLangCount() {
        if (languageConfigurator != null && viewModel != null) {
            languageConfigurator.refreshLangCount(lblLangCount, viewModel);
        }
    }

    public void updateSelectedSummary() {
        if (facade != null && viewModel != null) {
            var summary = facade.calculateSelectionSummary(viewModel.getCurrentGroups());
            viewModel.selectedSummaryTextProperty().set(summary.getFormattedSummary());
        }
    }

    public void startAnalysis() {
        if (viewModel != null && viewModel.analyzingProperty().get()) return;
        if (facade != null && viewModel != null) {
            try {
                facade.startAnalysis(viewModel.getLanguageDetectorMode());
            } catch (Exception e) {
                LOGGER.error("Error al iniciar el análisis de biblioteca", e);
                if (dialogService != null) {
                    dialogService.showError("Error de análisis", "No se pudo iniciar el análisis: " + e.getMessage());
                }
            }
        }
    }

    public void cancelAnalysis() {
        if (facade != null) {
            facade.cancelAnalysis();
        }
    }

    public void keepOriginals() {
        if (facade != null && viewModel != null) {
            facade.keepOriginals(viewModel.getCurrentGroups());
            refreshTable();
            updateSelectedSummary();
        }
    }

    public void selectDuplicates() {
        if (facade != null && viewModel != null) {
            facade.selectDuplicates(viewModel.getCurrentGroups());
            refreshTable();
            updateSelectedSummary();
        }
    }

    public void unselectAll() {
        if (facade != null && viewModel != null) {
            facade.unselectAll(viewModel.getCurrentGroups());
            refreshTable();
            updateSelectedSummary();
        }
    }

    public void moveSelectedToTrash() {
        if (facade != null && viewModel != null) {
            List<DuplicateCandidate> selected = facade.getSelectedCandidates(viewModel.getCurrentGroups());
            if (selected.isEmpty()) {
                if (dialogService != null) {
                    dialogService.showInfo("Sin selección", "No hay archivos marcados para mover a cuarentena.");
                }
                return;
            }

            var result = facade.moveSelectedToQuarantine(selected, viewModel.getCurrentGroups());
            if (dialogService != null) {
                dialogService.showInfo("Archivos movidos", String.format(
                        "Se movieron correctamente %d archivos a la carpeta de cuarentena (.duplicates).", result.countMoved()));
            }

            viewModel.updateGroups(result.remainingGroups());
            updateSelectedSummary();
        }
    }

    public void selectAllLanguages() {
        if (viewModel != null) {
            viewModel.selectAllVisible();
        }
    }

    public void deselectAllLanguages() {
        if (viewModel != null) {
            viewModel.deselectAllSongs();
        }
    }

    public void quarantineSelectedLanguages() {
        if (viewModel != null && facade != null) {
            List<SongFile> selected = viewModel.getSelectedSongsForQuarantine();
            if (selected.isEmpty()) {
                if (dialogService != null) {
                    dialogService.showInfo("Sin selección", "No hay canciones seleccionadas para mover a cuarentena.");
                }
                return;
            }

            var result = facade.moveSelectedSongsToQuarantine(selected);

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
    }

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
        if (facade != null) {
            facade.unsubscribeFromEvents();
        }
        LOGGER.info("LibraryAnalyzerPresenter liberado.");
    }
}
