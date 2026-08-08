package com.example.interfaz.service.analyzer.strategy;

import java.util.List;

import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.OriginalScoreCalculator.QualityEvaluation;

public class LargestFileStrategy implements OriginalSelectionStrategy {

    @Override
    public QualityEvaluation evaluate(SongFile song) {
        if (song == null) return new QualityEvaluation(0.0, List.of());
        double megabytes = song.getSize() / (1024.0 * 1024.0);
        double score = megabytes * 10.0;
        return new QualityEvaluation(score, List.of(String.format("Prioridad por tamaño: %.1f MB", megabytes)));
    }
}
