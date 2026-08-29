package com.example.interfaz.model.analyzer;

public enum LanguageDetectionMethod {
    TEXT("Texto"),
    UNICODE("Unicode"),
    METADATA("Metadatos"),
    AUDIO("Audio"),
    UNKNOWN("—");

    private final String displayName;

    LanguageDetectionMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
