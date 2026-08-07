package com.example.interfaz.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.*;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisCancelled;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisFinished;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisStarted;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryProgressUpdated;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.LibraryAnalysisResult;
import com.example.interfaz.service.analyzer.LibraryAnalyzerService;
import com.example.interfaz.service.ui.DialogService;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LibraryAnalyzerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryAnalyzerController.class);

    @FXML private Button btnStartAnalysis;
    @FXML private Button btnCancelAnalysis;
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

    @FXML private TreeTableView<AnalyzerRowModel> resultsTreeTable;
    @FXML private TreeTableColumn<AnalyzerRowModel, Boolean> colSelect;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colName;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colArtist;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colDuration;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colSize;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colType;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colStatus;
    @FXML private TreeTableColumn<AnalyzerRowModel, String> colPath;

    @FXML private Label selectedSummaryLabel;
    @FXML private Button btnKeepOriginal;
    @FXML private Button btnSelectDuplicates;
    @FXML private Button btnUnselectAll;
    @FXML private Button btnMoveToTrash;

    private LibraryAnalyzerService libraryAnalyzerService;
    private DialogService dialogService;
    private EventPublisher eventPublisher;

    private final List<DuplicateGroup> currentGroups = new ArrayList<>();

    @FXML
    void initialize() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.libraryAnalyzerService = factory.getLibraryAnalyzerService();
        this.dialogService = factory.getDialogService();
        this.eventPublisher = factory.getEventPublisher();

        setupTableColumns();
        registerEventSubscriptions();
    }

    private void setupTableColumns() {
        TreeItem<AnalyzerRowModel> root = new TreeItem<>(new AnalyzerRowModel("Root", null, null));
        resultsTreeTable.setRoot(root);
        resultsTreeTable.setShowRoot(false);

        colSelect.setCellValueFactory(param -> {
            AnalyzerRowModel model = param.getValue().getValue();
            if (model.isGroup()) {
                return new SimpleBooleanProperty(false);
            }
            SimpleBooleanProperty prop = new SimpleBooleanProperty(model.getCandidate().isSelectedForDeletion());
            prop.addListener((obs, oldVal, newVal) -> {
                model.getCandidate().setSelectedForDeletion(newVal);
                updateSelectedSummary();
            });
            return prop;
        });
        colSelect.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(colSelect));
        colSelect.setEditable(true);
        resultsTreeTable.setEditable(true);

        colName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getName()));
        colArtist.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getArtist()));
        colDuration.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getDurationFormatted()));
        colSize.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getSizeFormatted()));
        colType.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
        colStatus.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getStatus()));
        colPath.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getPath()));
    }

    private void registerEventSubscriptions() {
        if (eventPublisher != null) {
            eventPublisher.subscribe(LibraryAnalysisStarted.class, this::onAnalysisStartedEvent);
            eventPublisher.subscribe(LibraryProgressUpdated.class, this::onProgressUpdatedEvent);
            eventPublisher.subscribe(LibraryAnalysisFinished.class, this::onAnalysisFinishedEvent);
            eventPublisher.subscribe(LibraryAnalysisCancelled.class, this::onAnalysisCancelledEvent);
        }
    }

    private void onAnalysisStartedEvent(LibraryAnalysisStarted event) {
        Platform.runLater(() -> {
            btnStartAnalysis.setDisable(true);
            btnCancelAnalysis.setDisable(false);
            progressBox.setVisible(true);
            progressBox.setManaged(true);
            statsBox.setVisible(false);
            statsBox.setManaged(false);
            progressBar.setProgress(0.0);
            statusLabel.setText("Iniciando análisis en: " + event.getFolderPath());
            duplicatesFoundLabel.setText("Duplicados: 0");
            timeRemainingLabel.setText("Tiempo restante: --:--");
            clearResultsTable();
        });
    }

    private void onProgressUpdatedEvent(LibraryProgressUpdated event) {
        Platform.runLater(() -> {
            double progress = event.getTotalFiles() > 0
                    ? (double) event.getProcessedSongs() / event.getTotalFiles()
                    : 0.0;
            progressBar.setProgress(progress);
            statusLabel.setText(String.format("Analizando %d / %d canciones", event.getProcessedSongs(), event.getTotalFiles()));
            duplicatesFoundLabel.setText("Duplicados encontrados: " + event.getDuplicatesFound());
            timeRemainingLabel.setText("Tiempo restante: " + formatDuration(event.getEstimatedRemainingMillis()));
        });
    }

    private void onAnalysisFinishedEvent(LibraryAnalysisFinished event) {
        Platform.runLater(() -> {
            btnStartAnalysis.setDisable(false);
            btnCancelAnalysis.setDisable(true);
            progressBox.setVisible(false);
            progressBox.setManaged(false);
            displayResults(event.getResult());
        });
    }

    private void onAnalysisCancelledEvent(LibraryAnalysisCancelled event) {
        Platform.runLater(() -> {
            btnStartAnalysis.setDisable(false);
            btnCancelAnalysis.setDisable(true);
            progressBox.setVisible(false);
            progressBox.setManaged(false);
            displayResults(event.getPartialResult());
            if (dialogService != null) {
                dialogService.showInfo("Análisis cancelado", "El análisis fue cancelado por el usuario. Se muestran los resultados parciales.");
            }
        });
    }

    private void displayResults(LibraryAnalysisResult result) {
        if (result == null) return;

        statsBox.setVisible(true);
        statsBox.setManaged(true);
        lblStatTotalSongs.setText(String.valueOf(result.getTotalSongs()));
        lblStatTotalFiles.setText(String.valueOf(result.getTotalFiles()));
        lblStatGroups.setText(String.valueOf(result.getGroups().size()));
        lblStatRecoverableSpace.setText(formatSize(result.getRecoverableSpaceBytes()));
        lblStatDuration.setText(formatDuration(result.getTotalDurationMillis()));

        currentGroups.clear();
        currentGroups.addAll(result.getGroups());

        populateTreeTable(result.getGroups());
        updateSelectedSummary();
    }

    private void populateTreeTable(List<DuplicateGroup> groups) {
        TreeItem<AnalyzerRowModel> root = resultsTreeTable.getRoot();
        root.getChildren().clear();

        for (DuplicateGroup group : groups) {
            AnalyzerRowModel groupModel = new AnalyzerRowModel(
                    "▼ " + group.getGroupName() + " (" + group.getCandidates().size() + " archivos)",
                    group,
                    null
            );
            TreeItem<AnalyzerRowModel> groupItem = new TreeItem<>(groupModel);
            groupItem.setExpanded(true);

            for (DuplicateCandidate candidate : group.getCandidates()) {
                AnalyzerRowModel candidateModel = new AnalyzerRowModel(
                        candidate.getSongFile().getFileName(),
                        group,
                        candidate
                );
                TreeItem<AnalyzerRowModel> candidateItem = new TreeItem<>(candidateModel);
                groupItem.getChildren().add(candidateItem);
            }

            root.getChildren().add(groupItem);
        }
    }

    private void clearResultsTable() {
        if (resultsTreeTable.getRoot() != null) {
            resultsTreeTable.getRoot().getChildren().clear();
        }
        currentGroups.clear();
        updateSelectedSummary();
    }

    private void updateSelectedSummary() {
        int selectedCount = 0;
        long bytes = 0;
        for (DuplicateGroup group : currentGroups) {
            for (DuplicateCandidate candidate : group.getCandidates()) {
                if (candidate.isSelectedForDeletion()) {
                    selectedCount++;
                    bytes += candidate.getSongFile().getSize();
                }
            }
        }
        selectedSummaryLabel.setText(String.format("%d duplicados seleccionados (%s a liberar)", selectedCount, formatSize(bytes)));
    }

    @FXML
    void onStartAnalysis() {
        if (libraryAnalyzerService != null) {
            libraryAnalyzerService.startAnalysis();
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
        for (DuplicateGroup group : currentGroups) {
            List<DuplicateCandidate> candidates = group.getCandidates();
            if (candidates.isEmpty()) continue;

            DuplicateCandidate original = candidates.get(0);
            for (DuplicateCandidate c : candidates) {
                if (c.isOriginal()) {
                    original = c;
                    break;
                }
            }

            for (DuplicateCandidate c : candidates) {
                if (c == original) {
                    c.setSelectedForDeletion(false);
                } else {
                    c.setSelectedForDeletion(true);
                }
            }
        }
        populateTreeTable(currentGroups);
        updateSelectedSummary();
    }

    @FXML
    void onSelectDuplicates() {
        for (DuplicateGroup group : currentGroups) {
            for (DuplicateCandidate c : group.getCandidates()) {
                c.setSelectedForDeletion(!c.isOriginal());
            }
        }
        populateTreeTable(currentGroups);
        updateSelectedSummary();
    }

    @FXML
    void onUnselectAll() {
        for (DuplicateGroup group : currentGroups) {
            for (DuplicateCandidate c : group.getCandidates()) {
                c.setSelectedForDeletion(false);
            }
        }
        populateTreeTable(currentGroups);
        updateSelectedSummary();
    }

    @FXML
    void onMoveToTrash() {
        List<DuplicateCandidate> selected = new ArrayList<>();
        for (DuplicateGroup group : currentGroups) {
            for (DuplicateCandidate c : group.getCandidates()) {
                if (c.isSelectedForDeletion()) {
                    selected.add(c);
                }
            }
        }

        if (selected.isEmpty()) {
            if (dialogService != null) {
                dialogService.showInfo("Sin selección", "No hay archivos marcados para mover a cuarentena.");
            }
            return;
        }

        int moved = libraryAnalyzerService.moveToQuarantine(selected);
        if (dialogService != null) {
            dialogService.showInfo("Archivos movidos", String.format("Se movieron correctamente %d archivos a la carpeta de cuarentena (.duplicates).", moved));
        }

        // Re-filter removed items from group list
        List<DuplicateGroup> remainingGroups = new ArrayList<>();
        for (DuplicateGroup group : currentGroups) {
            List<DuplicateCandidate> remainingCandidates = new ArrayList<>();
            for (DuplicateCandidate candidate : group.getCandidates()) {
                if (!selected.contains(candidate)) {
                    remainingCandidates.add(candidate);
                }
            }
            if (remainingCandidates.size() > 1) {
                remainingGroups.add(new DuplicateGroup(group.getGroupName(), remainingCandidates, group.getClassification()));
            }
        }

        currentGroups.clear();
        currentGroups.addAll(remainingGroups);
        populateTreeTable(currentGroups);
        updateSelectedSummary();
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    public static class AnalyzerRowModel {
        private final String name;
        private final DuplicateGroup group;
        private final DuplicateCandidate candidate;

        public AnalyzerRowModel(String name, DuplicateGroup group, DuplicateCandidate candidate) {
            this.name = name;
            this.group = group;
            this.candidate = candidate;
        }

        public boolean isGroup() {
            return candidate == null;
        }

        public DuplicateCandidate getCandidate() {
            return candidate;
        }

        public String getName() {
            return name;
        }

        public String getArtist() {
            if (candidate != null) return candidate.getSongFile().getArtist();
            return "";
        }

        public String getDurationFormatted() {
            if (candidate != null) {
                long durationSec = candidate.getSongFile().getDuration();
                return String.format("%d:%02d", durationSec / 60, durationSec % 60);
            }
            return "";
        }

        public String getSizeFormatted() {
            if (candidate != null) {
                long bytes = candidate.getSongFile().getSize();
                if (bytes < 1024) return bytes + " B";
                int exp = (int) (Math.log(bytes) / Math.log(1024));
                char pre = "KMGTPE".charAt(exp - 1);
                return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
            }
            return "";
        }

        public String getType() {
            if (candidate != null) return candidate.getSongFile().getFormat();
            return "";
        }

        public String getStatus() {
            if (isGroup()) {
                return group.getClassification().getDisplayName();
            }
            return candidate.isOriginal() ? "Original" : (candidate.isSelectedForDeletion() ? "Eliminar copia" : "Conservar");
        }

        public String getPath() {
            if (candidate != null) return candidate.getSongFile().getPath().toString();
            return "";
        }
    }
}
