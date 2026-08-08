package com.example.interfaz.service.analyzer.strategy;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.OriginalScoreCalculator.QualityEvaluation;

public class OldestFileStrategy implements OriginalSelectionStrategy {

    @Override
    public QualityEvaluation evaluate(SongFile song) {
        if (song == null || song.getPath() == null) return new QualityEvaluation(0.0, List.of());
        long modifiedTime = 0;
        try {
            modifiedTime = Files.getLastModifiedTime(song.getPath()).toMillis();
        } catch (IOException ignored) {
        }

        // Older files get higher score (inverting timestamp delta from epoch)
        long current = System.currentTimeMillis();
        double ageDays = (current - modifiedTime) / (1000.0 * 60 * 60 * 24);
        double score = Math.max(0.0, ageDays);

        return new QualityEvaluation(score, List.of(String.format("Prioridad por antigüedad: %.0f días de antigüedad", ageDays)));
    }
}
