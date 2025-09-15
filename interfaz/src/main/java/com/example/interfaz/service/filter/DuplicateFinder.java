package com.example.interfaz.service.filter;

import java.util.*;
import java.util.logging.Logger;

/**
 * Buscador de duplicados que usa TitleNormalizer y SimilarityCalculator
 * Responsable de la lógica de detección de duplicados
 */
public class DuplicateFinder {
    
    private static final Logger LOGGER = Logger.getLogger(DuplicateFinder.class.getName());
    
    private final double similarityThreshold;
    
    /**
     * Constructor con umbral de similitud personalizado
     * @param similarityThreshold umbral entre 0.0 y 1.0
     */
    public DuplicateFinder(double similarityThreshold) {
        this.similarityThreshold = Math.max(0.0, Math.min(1.0, similarityThreshold));
    }
    
    /**
     * Constructor con umbral por defecto (0.70)
     */
    public DuplicateFinder() {
        this(0.70);
    }
    
    /**
     * Verifica si un título es duplicado en una colección
     * @param title título a verificar
     * @param existingTitles colección de títulos existentes
     * @return true si es duplicado
     */
    public boolean isDuplicate(String title, Collection<String> existingTitles) {
        if (title == null || title.trim().isEmpty() || existingTitles == null) {
            return false;
        }
        
        if (existingTitles.contains(title)) {
            LOGGER.info("Duplicado exacto encontrado: " + title);
            return true;
        }
        
        String normalizedTitle = TitleNormalizer.normalize(title);
        
        for (String existingTitle : existingTitles) {
            String normalizedExisting = TitleNormalizer.normalize(existingTitle);
            
            if (SimilarityCalculator.calculateCombinedSimilarity(normalizedTitle, normalizedExisting) >= similarityThreshold) {
                LOGGER.info("Similitud detectada: '" + title + "' es similar a '" + existingTitle + "'");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Encuentra el título más similar en una colección
     * @param title título objetivo
     * @param existingTitles colección de títulos existentes
     * @return el título más similar o null si no hay similitudes por encima del umbral
     */
    public String findMostSimilar(String title, Collection<String> existingTitles) {
        if (title == null || existingTitles == null || existingTitles.isEmpty()) {
            return null;
        }
        
        String normalizedTitle = TitleNormalizer.normalize(title);
        String mostSimilar = null;
        double maxSimilarity = 0.0;
        
        for (String existingTitle : existingTitles) {
            String normalizedExisting = TitleNormalizer.normalize(existingTitle);
            double similarity = SimilarityCalculator.calculateCombinedSimilarity(normalizedTitle, normalizedExisting);
            
            if (similarity >= similarityThreshold && similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilar = existingTitle;
            }
        }
        
        return mostSimilar;
    }
    
    /**
     * Agrupa títulos similares de una lista
     * @param titles lista de títulos
     * @return mapa de grupos de títulos similares
     */
    public Map<String, List<String>> groupSimilarTitles(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, List<String>> groups = new HashMap<>();
        Set<String> processed = new HashSet<>();
        
        for (String title : titles) {
            if (processed.contains(title)) {
                continue;
            }
            
            List<String> group = new ArrayList<>();
            group.add(title);
            processed.add(title);
            
            String normalizedTitle = TitleNormalizer.normalize(title);
            
            for (String otherTitle : titles) {
                if (!processed.contains(otherTitle)) {
                    String normalizedOther = TitleNormalizer.normalize(otherTitle);
                    
                    if (SimilarityCalculator.calculateCombinedSimilarity(normalizedTitle, normalizedOther) >= similarityThreshold) {
                        group.add(otherTitle);
                        processed.add(otherTitle);
                    }
                }
            }
            
            if (group.size() > 1) {
                groups.put(title, group);
            }
        }
        
        return groups;
    }
    
    /**
     * Filtra duplicados de una lista, manteniendo solo el primer título de cada grupo
     * @param titles lista de títulos
     * @return lista sin duplicados
     */
    public List<String> removeDuplicates(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> unique = new ArrayList<>();
        
        for (String title : titles) {
            if (!isDuplicate(title, unique)) {
                unique.add(title);
            }
        }
        
        return unique;
    }
    
    /**
     * Calcula estadísticas de duplicados en una lista
     * @param titles lista de títulos
     * @return mapa con estadísticas
     */
    public Map<String, Object> calculateDuplicateStats(List<String> titles) {
        Map<String, Object> stats = new HashMap<>();
        
        if (titles == null || titles.isEmpty()) {
            stats.put("totalTitles", 0);
            stats.put("uniqueTitles", 0);
            stats.put("duplicateGroups", 0);
            stats.put("duplicatePercentage", 0.0);
            return stats;
        }
        
        Map<String, List<String>> groups = groupSimilarTitles(titles);
        List<String> unique = removeDuplicates(titles);
        
        stats.put("totalTitles", titles.size());
        stats.put("uniqueTitles", unique.size());
        stats.put("duplicateGroups", groups.size());
        stats.put("duplicatePercentage", (double) (titles.size() - unique.size()) / titles.size() * 100);
        stats.put("similarityThreshold", similarityThreshold);
        
        return stats;
    }
    
    /**
     * Obtiene el umbral de similitud actual
     * @return umbral de similitud
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}