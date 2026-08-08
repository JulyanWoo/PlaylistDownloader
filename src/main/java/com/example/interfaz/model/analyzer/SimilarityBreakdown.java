package com.example.interfaz.model.analyzer;

public record SimilarityBreakdown(
    double titleScore,
    double artistScore,
    double durationScore,
    double languageAdjustment,
    double modifierPenalty,
    double finalScore
) {
    public String getFormattedSummary() {
        return String.format("Título: %.0f%% | Artista: %.0f%% | Duración: %.0f%% | Idioma: %+.0f%% | Modificador: -%.0f%% | Total: %.0f%%",
                titleScore * 100, artistScore * 100, durationScore * 100, languageAdjustment * 100, modifierPenalty * 100, finalScore * 100);
    }
}
