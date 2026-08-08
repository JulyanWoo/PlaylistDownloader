package com.example.interfaz.model.analyzer;

public record LanguageInfo(
    String code,
    String name,
    double confidence,
    long analyzedAtMillis
) {
    public static LanguageInfo unknown() {
        return new LanguageInfo("unknown", "Desconocido", 0.0, System.currentTimeMillis());
    }
}
