package com.example.interfaz.service.filter;

/**
 * Utilidad para normalizar títulos de canciones
 * Responsable únicamente de la normalización de texto
 */
public class TitleNormalizer {
    
    /**
     * Normaliza un título de canción para comparación
     * @param title título original
     * @return título normalizado
     */
    public static String normalize(String title) {
        if (title == null) {
            return "";
        }
        
        return title.toLowerCase()
                   .replaceAll("[\\[\\](){}]", "")
                   .replaceAll("\\s*(official|video|lyrics|audio|hd|4k|music|mv|clip)\\s*", "")
                   .replaceAll("[^a-z0-9\\s]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    /**
     * Normaliza múltiples títulos
     * @param titles array de títulos
     * @return array de títulos normalizados
     */
    public static String[] normalize(String... titles) {
        String[] normalized = new String[titles.length];
        for (int i = 0; i < titles.length; i++) {
            normalized[i] = normalize(titles[i]);
        }
        return normalized;
    }
    
    /**
     * Verifica si un título está normalizado
     * @param title título a verificar
     * @return true si ya está normalizado
     */
    public static boolean isNormalized(String title) {
        if (title == null) {
            return false;
        }
        
        String normalized = normalize(title);
        return title.equals(normalized);
    }
    
    /**
     * Extrae palabras clave de un título normalizado
     * @param normalizedTitle título ya normalizado
     * @return array de palabras clave
     */
    public static String[] extractKeywords(String normalizedTitle) {
        if (normalizedTitle == null || normalizedTitle.trim().isEmpty()) {
            return new String[0];
        }
        
        return normalizedTitle.split("\\s+");
    }
}