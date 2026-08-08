package com.example.interfaz.service.ui.analyzer;

import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.util.FormatUtils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * JavaFX observable wrapper for a SongFile used in the Language Browser TableView.
 * Includes a BooleanProperty for row-level checkbox selection.
 */
public class LanguageSongRowModel {

    private final SongFile songFile;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public LanguageSongRowModel(SongFile songFile) {
        this.songFile = songFile;
    }

    public SongFile getSongFile() {
        return songFile;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    // ── Display helpers ──────────────────────────────────────────────────────

    public String getName() {
        if (songFile.getTitle() != null && !songFile.getTitle().isBlank()) {
            return songFile.getTitle();
        }
        return songFile.getFileName();
    }

    public String getArtist() {
        String artist = songFile.getArtist();
        return (artist != null && !artist.isBlank()) ? artist : "—";
    }

    public String getLanguage() {
        return songFile.getLanguageInfo().name();
    }

    public String getConfidence() {
        double conf = songFile.getLanguageInfo().confidence();
        if (conf <= 0.0) return "—";
        return String.format("%.0f%%", conf * 100);
    }

    public String getFormat() {
        String fmt = songFile.getFormat();
        return (fmt != null && !fmt.isBlank()) ? fmt : "—";
    }

    public String getSizeFormatted() {
        return FormatUtils.formatSize(songFile.getSize());
    }

    public String getPath() {
        return songFile.getPath() != null ? songFile.getPath().toString() : "—";
    }
}
