package com.example.interfaz.model.analyzer;

public enum GroupClassification {
    CONFIRMED_DUPLICATE("Duplicado confirmado"),
    VERSION_VARIANT("Variante de versión"),
    POSSIBLE_DUPLICATE("Posible duplicado"),
    SIMILAR_FILES("Archivos similares");

    private final String displayName;

    GroupClassification(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
