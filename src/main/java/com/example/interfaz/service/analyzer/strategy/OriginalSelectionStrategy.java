package com.example.interfaz.service.analyzer.strategy;

import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.OriginalScoreCalculator.QualityEvaluation;

public interface OriginalSelectionStrategy {
    QualityEvaluation evaluate(SongFile song);
}
