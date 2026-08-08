package com.example.interfaz.model.analyzer;

public enum AnalysisStage {
    SCANNING("Escaneando archivos"),
    READING_METADATA("Leyendo metadatos e ID3 tags"),
    HASHING("Comprobando hashes e integridad"),
    COMPARING("Comparando similitud y variaciones"),
    BUILDING_RESULTS("Generando recomendaciones");

    private final String displayName;

    AnalysisStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
