package com.example.interfaz.viewmodel;

import java.util.ArrayList;
import java.util.List;

import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.model.analyzer.LibraryAnalysisResult;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.ui.analyzer.LanguageSongRowModel;
import com.example.interfaz.util.FormatUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

public class LibraryAnalyzerViewModel {

    // ── Analysis state ────────────────────────────────────────────────────────

    private final BooleanProperty analyzing = new SimpleBooleanProperty(false);
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final StringProperty statusText = new SimpleStringProperty("");
    private final StringProperty duplicatesFoundText = new SimpleStringProperty("Duplicados: 0");
    private final StringProperty timeRemainingText = new SimpleStringProperty("Tiempo restante: --:--");

    private final ObjectProperty<LanguageDetectorMode> languageDetectorMode = new SimpleObjectProperty<>(LanguageDetectorMode.BALANCED);

    private final BooleanProperty statsVisible = new SimpleBooleanProperty(false);
    private final StringProperty statTotalSongsText = new SimpleStringProperty("0");
    private final StringProperty statTotalFilesText = new SimpleStringProperty("0");
    private final StringProperty statGroupsText = new SimpleStringProperty("0");
    private final StringProperty statRecoverableSpaceText = new SimpleStringProperty("0 B");
    private final StringProperty statDurationText = new SimpleStringProperty("00:00");

    private final StringProperty selectedSummaryText = new SimpleStringProperty("0 duplicados seleccionados (0 B a liberar)");

    // ── Duplicate groups ──────────────────────────────────────────────────────

    private final ObservableList<DuplicateGroup> currentGroups = FXCollections.observableArrayList();

    // ── Language Browser state ────────────────────────────────────────────────

    private final ObservableList<LanguageSongRowModel> allSongModels = FXCollections.observableArrayList();
    private final FilteredList<LanguageSongRowModel> filteredSongModels = new FilteredList<>(allSongModels, p -> true);

    /** Currently selected language filter ("Todos" means no filter). */
    private final StringProperty langFilter = new SimpleStringProperty("Todos");
    /** Free-text search applied to name / artist / path. */
    private final StringProperty langSearch = new SimpleStringProperty("");
    /** Summary label for the language browser bottom bar. */
    private final StringProperty langBrowserSummaryText = new SimpleStringProperty("0 canciones seleccionadas");

    public LibraryAnalyzerViewModel() {
        langFilter.addListener((obs, old, nv) -> updateLangFilter());
        langSearch.addListener((obs, old, nv) -> updateLangFilter());
    }

    // ── Language Browser ──────────────────────────────────────────────────────

    public FilteredList<LanguageSongRowModel> getFilteredSongModels() {
        return filteredSongModels;
    }

    public ObservableList<LanguageSongRowModel> getAllSongModels() {
        return allSongModels;
    }

    public StringProperty langFilterProperty() {
        return langFilter;
    }

    public StringProperty langSearchProperty() {
        return langSearch;
    }

    public StringProperty langBrowserSummaryTextProperty() {
        return langBrowserSummaryText;
    }

    public void updateLangBrowserSummary() {
        long selected = allSongModels.stream().filter(LanguageSongRowModel::isSelected).count();
        if (selected == 0) {
            langBrowserSummaryText.set("0 canciones seleccionadas");
        } else {
            long totalBytes = allSongModels.stream()
                    .filter(LanguageSongRowModel::isSelected)
                    .mapToLong(m -> m.getSongFile().getSize())
                    .sum();
            langBrowserSummaryText.set(String.format("%d canción(es) seleccionada(s) (%s)",
                    selected, FormatUtils.formatSize(totalBytes)));
        }
    }

    /** Returns all song models currently selected for quarantine. */
    public List<SongFile> getSelectedSongsForQuarantine() {
        List<SongFile> result = new ArrayList<>();
        for (LanguageSongRowModel m : allSongModels) {
            if (m.isSelected()) {
                result.add(m.getSongFile());
            }
        }
        return result;
    }

    /** Selects all visible (filtered) song rows. */
    public void selectAllVisible() {
        filteredSongModels.forEach(m -> m.setSelected(true));
        updateLangBrowserSummary();
    }

    /** Deselects all song rows. */
    public void deselectAllSongs() {
        allSongModels.forEach(m -> m.setSelected(false));
        updateLangBrowserSummary();
    }

    /** Removes quarantined songs from the list after a successful move. */
    public void removeSongsFromBrowser(List<SongFile> movedSongs) {
        allSongModels.removeIf(m -> movedSongs.contains(m.getSongFile()));
    }

    private void updateLangFilter() {
        String filter = langFilter.get();
        String search = langSearch.get();

        filteredSongModels.setPredicate(model -> {
            // Language filter
            if (filter != null && !filter.equals("Todos")) {
                if (!model.getSongFile().getLanguageInfo().name().equals(filter)) {
                    return false;
                }
            }
            // Text search
            if (search != null && !search.isBlank()) {
                String lower = search.toLowerCase();
                SongFile sf = model.getSongFile();
                boolean nameMatch = sf.getFileName().toLowerCase().contains(lower)
                        || (sf.getTitle() != null && sf.getTitle().toLowerCase().contains(lower))
                        || (sf.getArtist() != null && sf.getArtist().toLowerCase().contains(lower));
                if (!nameMatch) return false;
            }
            return true;
        });
    }

    // ── Properties (existing) ─────────────────────────────────────────────────

    public ObjectProperty<LanguageDetectorMode> languageDetectorModeProperty() {
        return languageDetectorMode;
    }

    public LanguageDetectorMode getLanguageDetectorMode() {
        return languageDetectorMode.get();
    }

    public void setLanguageDetectorMode(LanguageDetectorMode mode) {
        this.languageDetectorMode.set(mode != null ? mode : LanguageDetectorMode.BALANCED);
    }

    public BooleanProperty analyzingProperty() {
        return analyzing;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public StringProperty duplicatesFoundTextProperty() {
        return duplicatesFoundText;
    }

    public StringProperty timeRemainingTextProperty() {
        return timeRemainingText;
    }

    public BooleanProperty statsVisibleProperty() {
        return statsVisible;
    }

    public StringProperty statTotalSongsTextProperty() {
        return statTotalSongsText;
    }

    public StringProperty statTotalFilesTextProperty() {
        return statTotalFilesText;
    }

    public StringProperty statGroupsTextProperty() {
        return statGroupsText;
    }

    public StringProperty statRecoverableSpaceTextProperty() {
        return statRecoverableSpaceText;
    }

    public StringProperty statDurationTextProperty() {
        return statDurationText;
    }

    public StringProperty selectedSummaryTextProperty() {
        return selectedSummaryText;
    }

    public ObservableList<DuplicateGroup> getCurrentGroups() {
        return currentGroups;
    }

    public void startAnalysis(String folderPath) {
        analyzing.set(true);
        progress.set(0.0);
        statusText.set("Iniciando análisis en: " + folderPath);
        duplicatesFoundText.set("Duplicados: 0");
        timeRemainingText.set("Tiempo restante: --:--");
        statsVisible.set(false);
        currentGroups.clear();
        allSongModels.clear();
        langBrowserSummaryText.set("0 canciones seleccionadas");
    }

    public void updateProgress(long processed, long total, long duplicatesFound, long remainingMillis) {
        double p = total > 0 ? (double) processed / total : 0.0;
        progress.set(p);
        statusText.set(String.format("Analizando %d / %d canciones", processed, total));
        duplicatesFoundText.set("Duplicados encontrados: " + duplicatesFound);
        timeRemainingText.set("Tiempo restante: " + FormatUtils.formatDuration(remainingMillis));
    }

    public void finishAnalysis(LibraryAnalysisResult result) {
        analyzing.set(false);
        displayResults(result);
    }

    public void cancelAnalysis(LibraryAnalysisResult partialResult) {
        analyzing.set(false);
        displayResults(partialResult);
    }

    public void displayResults(LibraryAnalysisResult result) {
        if (result == null) {
            return;
        }

        statsVisible.set(true);
        statTotalSongsText.set(String.valueOf(result.getTotalSongs()));
        statTotalFilesText.set(String.valueOf(result.getTotalFiles()));
        statGroupsText.set(String.valueOf(result.getGroups().size()));
        statRecoverableSpaceText.set(FormatUtils.formatSize(result.getRecoverableSpaceBytes()));
        statDurationText.set(FormatUtils.formatDuration(result.getTotalDurationMillis()));

        currentGroups.setAll(result.getGroups());

        // Populate Language Browser
        List<LanguageSongRowModel> models = new ArrayList<>();
        if (result.getAllSongs() != null) {
            for (SongFile song : result.getAllSongs()) {
                models.add(new LanguageSongRowModel(song));
            }
        }
        allSongModels.setAll(models);
        langBrowserSummaryText.set("0 canciones seleccionadas");
    }

    public void updateGroups(List<DuplicateGroup> newGroups) {
        currentGroups.setAll(newGroups != null ? newGroups : new ArrayList<>());
    }
}
