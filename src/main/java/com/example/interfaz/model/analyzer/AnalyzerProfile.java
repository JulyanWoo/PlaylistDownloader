package com.example.interfaz.model.analyzer;

public enum AnalyzerProfile {
    CONSERVATIVE("Conservador", 0.98, 0.85),
    BALANCED("Balanceado", 0.95, 0.75),
    AGGRESSIVE("Profundo / Agresivo", 0.90, 0.65);

    private final String displayName;
    private final double confirmedThreshold;
    private final double possibleThreshold;

    AnalyzerProfile(String displayName, double confirmedThreshold, double possibleThreshold) {
        this.displayName = displayName;
        this.confirmedThreshold = confirmedThreshold;
        this.possibleThreshold = possibleThreshold;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getConfirmedThreshold() {
        return confirmedThreshold;
    }

    public double getPossibleThreshold() {
        return possibleThreshold;
    }
}
