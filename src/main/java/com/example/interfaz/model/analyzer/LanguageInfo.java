package com.example.interfaz.model.analyzer;

public record LanguageInfo(
    String code,
    String name,
    double confidence,
    double margin,
    LanguageDetectionMethod method,
    String alternatives,
    long analyzedAtMillis
) {
    public LanguageInfo(String code, String name, double confidence, long analyzedAtMillis) {
        this(code, name, confidence, 0.0, LanguageDetectionMethod.UNKNOWN, "", analyzedAtMillis);
    }

    public static LanguageInfo unknown() {
        return new LanguageInfo("unknown", "Desconocido", 0.0, 0.0,
                LanguageDetectionMethod.UNKNOWN, "", System.currentTimeMillis());
    }
}
