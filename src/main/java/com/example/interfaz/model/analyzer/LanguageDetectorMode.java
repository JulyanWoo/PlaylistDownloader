package com.example.interfaz.model.analyzer;

public enum LanguageDetectorMode {
    CONSERVATIVE("Estricta (Baja)"),
    BALANCED("Balanceada (Media)"),
    AGGRESSIVE("Agresiva (Alta)");

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
