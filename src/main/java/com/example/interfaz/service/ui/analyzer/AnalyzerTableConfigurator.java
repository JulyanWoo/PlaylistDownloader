package com.example.interfaz.service.ui.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.kordamp.ikonli.javafx.FontIcon;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.util.FormatUtils;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Renders duplicate groups as compact cards so the recommended original,
 * the copies and their selection state are readable at a glance.
 */
public class AnalyzerTableConfigurator {

    private Runnable onSelectionChanged = () -> {};
    private Consumer<DuplicateCandidate> onPreviewRequested = candidate -> {};
    private Predicate<DuplicateCandidate> isPreviewing = candidate -> false;

    public void configure(
            VBox duplicateGroupsContainer,
            Runnable onSelectionChanged,
            Consumer<DuplicateCandidate> onPreviewRequested,
            Predicate<DuplicateCandidate> isPreviewing
    ) {
        this.onSelectionChanged = onSelectionChanged != null ? onSelectionChanged : () -> {};
        this.onPreviewRequested = onPreviewRequested != null ? onPreviewRequested : candidate -> {};
        this.isPreviewing = isPreviewing != null ? isPreviewing : candidate -> false;

        if (duplicateGroupsContainer != null) {
            duplicateGroupsContainer.setFillWidth(true);
        }
    }

    public void populate(VBox duplicateGroupsContainer, List<DuplicateGroup> groups) {
        if (duplicateGroupsContainer == null) {
            return;
        }

        duplicateGroupsContainer.getChildren().clear();
        if (groups == null || groups.isEmpty()) {
            duplicateGroupsContainer.getChildren().add(createEmptyState());
            return;
        }

        for (DuplicateGroup group : groups) {
            if (group != null) {
                duplicateGroupsContainer.getChildren().add(createGroupCard(group));
            }
        }
    }

    private VBox createGroupCard(DuplicateGroup group) {
        VBox card = new VBox(8);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("duplicate-group-card");

        List<DuplicateCandidate> candidates = group.getCandidates() != null ? group.getCandidates() : List.of();
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("duplicate-group-header");

        CheckBox groupSelection = createGroupSelection(candidates);
        VBox titleBox = new VBox(2);
        titleBox.setMinWidth(0);
        titleBox.setMaxWidth(Double.MAX_VALUE);
        Label title = new Label("GRUPO · " + textOr(group.getGroupName(), "Canciones parecidas"));
        title.getStyleClass().add("duplicate-group-title");
        title.setMaxWidth(Double.MAX_VALUE);

        String classification = group.getClassification() == null
                ? "Grupo de canciones parecidas"
                : group.getClassification().getDisplayName();
        Label subtitle = new Label(classification + " · " + group.getConfidencePercentage() + "% de coincidencia");
        subtitle.getStyleClass().add("duplicate-group-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        int copies = (int) candidates.stream().filter(candidate -> !candidate.isOriginal()).count();
        Label copiesLabel = new Label(copies + (copies == 1 ? " copia" : " copias"));
        copiesLabel.getStyleClass().add("duplicate-group-count");

        header.getChildren().addAll(groupSelection, titleBox, copiesLabel);
        card.getChildren().add(header);

        for (DuplicateCandidate candidate : candidates) {
            if (candidate != null && candidate.getSongFile() != null) {
                card.getChildren().add(createTrackRow(candidate));
            }
        }

        return card;
    }

    private CheckBox createGroupSelection(List<DuplicateCandidate> candidates) {
        List<DuplicateCandidate> copies = candidates.stream()
                .filter(candidate -> !candidate.isOriginal())
                .toList();

        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("duplicate-group-checkbox");
        checkBox.setFocusTraversable(false);
        checkBox.setDisable(copies.isEmpty());
        checkBox.setSelected(!copies.isEmpty() && copies.stream().allMatch(DuplicateCandidate::isSelectedForDeletion));
        checkBox.setTooltip(new Tooltip("Marcar o desmarcar todas las copias de este grupo"));
        checkBox.setOnAction(event -> {
            boolean selected = checkBox.isSelected();
            copies.forEach(candidate -> candidate.setSelectedForDeletion(selected));
            notifySelectionChanged();
        });
        return checkBox;
    }

    private HBox createTrackRow(DuplicateCandidate candidate) {
        boolean original = candidate.isOriginal();
        boolean selectedForDeletion = candidate.isSelectedForDeletion();

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("duplicate-track-row");
        row.getStyleClass().add(original
                ? "duplicate-track-original"
                : selectedForDeletion ? "duplicate-track-copy-selected" : "duplicate-track-copy");

        if (original) {
            Label originalMarker = new Label();
            originalMarker.setGraphic(createIcon("mdi2c-check", 13));
            originalMarker.getStyleClass().add("duplicate-original-marker");
            originalMarker.setTooltip(new Tooltip("Archivo recomendado para conservar"));
            row.getChildren().add(originalMarker);
        } else {
            CheckBox copySelection = new CheckBox();
            copySelection.getStyleClass().add("duplicate-copy-checkbox");
            copySelection.setFocusTraversable(false);
            copySelection.setSelected(selectedForDeletion);
            copySelection.setTooltip(new Tooltip(selectedForDeletion
                    ? "Esta copia se moverá a cuarentena"
                    : "Marcar esta copia para mover a cuarentena"));
            copySelection.setOnAction(event -> {
                candidate.setSelectedForDeletion(copySelection.isSelected());
                notifySelectionChanged();
            });
            row.getChildren().add(copySelection);
        }

        Label roleBadge = new Label(original ? "ORIGINAL" : selectedForDeletion ? "COPIA MARCADA" : "COPIA");
        roleBadge.getStyleClass().add("duplicate-status-badge");
        roleBadge.getStyleClass().add(original
                ? "duplicate-status-original"
                : selectedForDeletion ? "duplicate-status-copy-selected" : "duplicate-status-copy");
        row.getChildren().add(roleBadge);

        row.getChildren().add(createPreviewButton(candidate, "Escuchar esta versión"));

        VBox trackInfo = new VBox(3);
        trackInfo.setMinWidth(0);
        trackInfo.setMaxWidth(Double.MAX_VALUE);

        Label trackName = new Label(displayTrackName(candidate.getSongFile()));
        trackName.getStyleClass().add("duplicate-track-name");
        trackName.setMinWidth(0);
        trackName.setMaxWidth(Double.MAX_VALUE);

        HBox details = new HBox(8);
        details.setAlignment(Pos.CENTER_LEFT);
        Label metadata = new Label(formatMetadata(candidate));
        metadata.getStyleClass().add("duplicate-track-meta");
        Label similarity = new Label(original
                ? "Mejor versión"
                : candidate.getSimilarityPercentage() + "% similar");
        similarity.getStyleClass().add("duplicate-track-similarity");
        details.getChildren().addAll(metadata, similarity);

        trackInfo.getChildren().addAll(trackName, details);
        HBox.setHgrow(trackInfo, Priority.ALWAYS);

        Label path = new Label(FormatUtils.abbreviatePath(candidate.getSongFile().getPath().toString(), 46));
        path.getStyleClass().add("duplicate-track-path");
        path.setMinWidth(0);
        path.setMaxWidth(310);
        path.setTooltip(new Tooltip(candidate.getSongFile().getPath().toString()));

        row.getChildren().addAll(trackInfo, path);
        return row;
    }

    private Button createPreviewButton(DuplicateCandidate candidate, String tooltipText) {
        boolean enabled = candidate != null
                && candidate.getSongFile() != null
                && AudioPreviewService.isPreviewAvailable(candidate.getSongFile().getPath());
        boolean playing = enabled && isPreviewing.test(candidate);

        Button button = new Button();
        button.setFocusTraversable(false);
        button.setMinSize(30, 30);
        button.setPrefSize(30, 30);
        button.setMaxSize(30, 30);
        button.getStyleClass().add("audio-preview-button");
        if (playing) {
            button.getStyleClass().add("audio-preview-playing");
        }
        button.setGraphic(createIcon(playing ? "mdi2p-pause" : "mdi2p-play", 14));
        button.setDisable(!enabled);
        button.setTooltip(new Tooltip(enabled
                ? tooltipText
                : "Vista previa disponible para MP3, AAC/M4A y WAV"));
        button.setOnAction(event -> {
            if (enabled) {
                onPreviewRequested.accept(candidate);
            }
        });
        return button;
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(6);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setMinHeight(170);
        emptyState.getStyleClass().add("empty-duplicates-state");

        FontIcon icon = createIcon("mdi2f-folder-search-outline", 28);
        icon.getStyleClass().add("empty-duplicates-icon");
        Label title = new Label("Aún no hay grupos para revisar");
        title.getStyleClass().add("empty-duplicates-title");
        Label description = new Label("Analiza tu carpeta de música para comparar las canciones y detectar copias.");
        description.getStyleClass().add("empty-duplicates-description");
        description.setWrapText(true);
        description.setMaxWidth(420);

        emptyState.getChildren().addAll(icon, title, description);
        return emptyState;
    }

    private void notifySelectionChanged() {
        onSelectionChanged.run();
    }

    private FontIcon createIcon(String iconLiteral, int size) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(size);
        return icon;
    }

    private String displayTrackName(SongFile songFile) {
        String title = textOr(songFile.getTitle(), stripExtension(songFile.getFileName()));
        String artist = songFile.getArtist();
        return artist == null || artist.isBlank() ? title : artist + " — " + title;
    }

    private String formatMetadata(DuplicateCandidate candidate) {
        SongFile songFile = candidate.getSongFile();
        List<String> details = new ArrayList<>();
        if (songFile.getFormat() != null && !songFile.getFormat().isBlank()) {
            details.add(songFile.getFormat().toUpperCase());
        }
        if (songFile.getBitrate() != null && !songFile.getBitrate().isBlank()) {
            details.add(songFile.getBitrate());
        }
        details.add(FormatUtils.formatSize(songFile.getSize()));
        details.add(FormatUtils.formatDurationSeconds(songFile.getDuration()));
        return String.join(" · ", details);
    }

    private String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Archivo de audio";
        }
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
