package com.example.interfaz.service.ui.analyzer;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.util.FormatUtils;

public class AnalyzerRowModel {

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

    public DuplicateGroup getGroup() {
        return group;
    }

    public DuplicateCandidate getCandidate() {
        return candidate;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        if (candidate != null) {
            return candidate.getSongFile().getArtist();
        }
        return "";
    }

    public String getDurationFormatted() {
        if (candidate != null) {
            return FormatUtils.formatDurationSeconds(candidate.getSongFile().getDuration());
        }
        return "";
    }

    public String getSizeFormatted() {
        if (candidate != null) {
            return FormatUtils.formatSize(candidate.getSongFile().getSize());
        }
        return "";
    }

    public String getType() {
        if (candidate != null) {
            return candidate.getSongFile().getFormat();
        }
        return "";
    }

    public String getStatus() {
        if (isGroup()) {
            return String.format("%s (%d%% conf)", group.getClassification().getDisplayName(), group.getConfidencePercentage());
        }
        int simPct = candidate.getSimilarityPercentage();
        if (candidate.isOriginal()) {
            return String.format("Original (%d pts)", candidate.getOriginalScore());
        }
        String action = candidate.isSelectedForDeletion() ? "Eliminar copia" : "Conservar";
        return String.format("%s (%d%% sim)", action, simPct);
    }

    public String getPath() {
        if (candidate != null) {
            return candidate.getSongFile().getPath().toString();
        }
        return "";
    }

    public String getLanguageFormatted() {
        if (candidate != null && candidate.getDetectedLanguage() != null) {
            int pct = (int) Math.round(candidate.getLanguageConfidence() * 100);
            return String.format("%s (%d%%)", candidate.getDetectedLanguage(), pct);
        }
        return "";
    }
}
