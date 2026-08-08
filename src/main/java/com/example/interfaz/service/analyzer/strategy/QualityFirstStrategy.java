package com.example.interfaz.service.analyzer.strategy;

import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.OriginalScoreCalculator;
import com.example.interfaz.service.analyzer.OriginalScoreCalculator.QualityEvaluation;

public class QualityFirstStrategy implements OriginalSelectionStrategy {

    private final OriginalScoreCalculator calculator = new OriginalScoreCalculator();

    @Override
    public QualityEvaluation evaluate(SongFile song) {
        return calculator.evaluateQuality(song);
    }
}
