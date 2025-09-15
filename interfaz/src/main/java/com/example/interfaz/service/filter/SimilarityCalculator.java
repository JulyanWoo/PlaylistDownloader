package com.example.interfaz.service.filter;

import java.util.*;

/**
 * Calculadora de similitud entre títulos
 * Responsable únicamente de los cálculos de distancia y similitud
 */
public class SimilarityCalculator {
    
    /**
     * Calcula la similitud combinada entre dos títulos normalizados
     * @param normalizedTitle1 primer título normalizado
     * @param normalizedTitle2 segundo título normalizado
     * @return porcentaje de similitud (0.0 a 1.0)
     */
    public static double calculateCombinedSimilarity(String normalizedTitle1, String normalizedTitle2) {
        if (normalizedTitle1 == null || normalizedTitle2 == null) {
            return 0.0;
        }
        
        if (normalizedTitle1.equals(normalizedTitle2)) {
            return 1.0;
        }
        
        double levenshteinSim = calculateLevenshteinSimilarity(normalizedTitle1, normalizedTitle2);
        double jaccardSim = calculateJaccardSimilarity(normalizedTitle1, normalizedTitle2);
        double containmentSim = calculateContainmentSimilarity(normalizedTitle1, normalizedTitle2);
        
        return (levenshteinSim * 0.4) + (jaccardSim * 0.3) + (containmentSim * 0.3);
    }
    
    /**
     * Calcula similitud usando distancia de Levenshtein
     * @param s1 primera cadena
     * @param s2 segunda cadena
     * @return similitud entre 0.0 y 1.0
     */
    public static double calculateLevenshteinSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * Calcula similitud usando índice de Jaccard
     * @param s1 primera cadena
     * @param s2 segunda cadena
     * @return similitud entre 0.0 y 1.0
     */
    public static double calculateJaccardSimilarity(String s1, String s2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Calcula similitud de contención (una cadena contiene a la otra)
     * @param s1 primera cadena
     * @param s2 segunda cadena
     * @return similitud entre 0.0 y 1.0
     */
    public static double calculateContainmentSimilarity(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        
        if (s1.contains(s2) || s2.contains(s1)) {
            return 0.8; 
        }
        
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        int matches = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.length() > 2 && word2.length() > 2 && 
                    (word1.contains(word2) || word2.contains(word1))) {
                    matches++;
                }
            }
        }
        
        return (double) matches / Math.max(words1.length, words2.length);
    }
    
    /**
     * Calcula la distancia de Levenshtein entre dos cadenas
     * @param s1 primera cadena
     * @param s2 segunda cadena
     * @return distancia de edición
     */
    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Verifica si dos títulos son similares según un umbral
     * @param title1 primer título
     * @param title2 segundo título
     * @param threshold umbral de similitud (0.0 a 1.0)
     * @return true si son similares
     */
    public static boolean areSimilar(String title1, String title2, double threshold) {
        String normalized1 = TitleNormalizer.normalize(title1);
        String normalized2 = TitleNormalizer.normalize(title2);
        
        return calculateCombinedSimilarity(normalized1, normalized2) >= threshold;
    }
    
    /**
     * Encuentra la cadena más similar de una lista
     * @param target cadena objetivo
     * @param candidates lista de candidatos
     * @return la cadena más similar o null si no hay candidatos
     */
    public static String findMostSimilar(String target, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        
        String normalizedTarget = TitleNormalizer.normalize(target);
        String mostSimilar = null;
        double maxSimilarity = 0.0;
        
        for (String candidate : candidates) {
            String normalizedCandidate = TitleNormalizer.normalize(candidate);
            double similarity = calculateCombinedSimilarity(normalizedTarget, normalizedCandidate);
            
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilar = candidate;
            }
        }
        
        return mostSimilar;
    }
}