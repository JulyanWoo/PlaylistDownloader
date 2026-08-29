package com.example.interfaz.model.analyzer;

public enum LanguageDetectorMode {
    FAST("Rápido"),
    BALANCED("Balanceado"),
    PRECISE("Preciso");

    private final String displayName;

    LanguageDetectorMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
