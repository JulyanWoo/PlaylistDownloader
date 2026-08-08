package com.example.interfaz.service.analyzer;

import java.util.ArrayList;
import java.util.List;

import com.example.interfaz.model.analyzer.SongFile;

public class OriginalScoreCalculator {

    public record QualityEvaluation(double score, List<String> reasons) {}

    public QualityEvaluation evaluateQuality(SongFile song) {
        if (song == null) return new QualityEvaluation(0.0, List.of());
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // +40 pts: Full ID3 Metadata completeness
        boolean hasArtist = song.getArtist() != null && !song.getArtist().isBlank();
        boolean hasTitle = song.getTitle() != null && !song.getTitle().isBlank();
        if (hasArtist && hasTitle) {
            score += 40;
            reasons.add("Metadata ID3 completa (+40)");
        } else if (hasTitle) {
            score += 20;
            reasons.add("Título ID3 presente (+20)");
        }

        // +30 pts: Bitrate quality
        String bitrate = song.getBitrate() != null ? song.getBitrate().toLowerCase() : "";
        if (bitrate.contains("320")) {
            score += 30;
            reasons.add("Calidad de audio 320kbps (+30)");
        } else if (bitrate.contains("256") || bitrate.contains("224")) {
            score += 20;
            reasons.add("Calidad de audio 256kbps (+20)");
        } else if (bitrate.contains("192") || bitrate.contains("160")) {
            score += 10;
            reasons.add("Calidad de audio 192kbps (+10)");
        }

        // +20 pts: Clean filename
        String fn = song.getFileName() != null ? song.getFileName().toLowerCase() : "";
        boolean dirty = fn.contains("copy") || fn.contains("copia") || fn.contains("(1)") || fn.contains("(2)") || fn.contains("official video") || fn.contains("vevo") || fn.contains("4k");
        if (!dirty) {
            score += 20;
            reasons.add("Nombre de archivo limpio (+20)");
        } else {
            reasons.add("Etiquetas/sufijos en nombre (-10)");
        }

        // +10 pts: Relative size
        double sizeScore = Math.min(10, (song.getSize() / (1024.0 * 1024.0)));
        score += sizeScore;
        reasons.add(String.format("Tamaño relativo %.1f MB", song.getSize() / (1024.0 * 1024.0)));

        return new QualityEvaluation(score, reasons);
    }
}
