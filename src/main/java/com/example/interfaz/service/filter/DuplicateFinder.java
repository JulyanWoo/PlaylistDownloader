package com.example.interfaz.service.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DuplicateFinder {

    private static final Logger LOGGER = Logger.getLogger(DuplicateFinder.class.getName());

    private final double similarityThreshold;

    public DuplicateFinder(double similarityThreshold) {
        this.similarityThreshold = Math.max(0.0, Math.min(1.0, similarityThreshold));
    }

    public DuplicateFinder() {
        this(0.70);
    }

    public boolean isDuplicate(String title, Collection<String> existingTitles) {
        if (title == null || title.trim().isEmpty() || existingTitles == null) {
            return false;
        }

        if (existingTitles.contains(title)) {
            LOGGER.info(() -> "Duplicado exacto encontrado: " + title);
            return true;
        }

        String normalizedTitle = TitleNormalizer.normalize(title);

        for (String existingTitle : existingTitles) {
            String normalizedExisting = TitleNormalizer.normalize(existingTitle);

            if (SimilarityCalculator.calculateCombinedSimilarity(normalizedTitle, normalizedExisting) >= similarityThreshold) {
                LOGGER.info(() -> "Similitud detectada: '" + title + "' es similar a '" + existingTitle + "'");
                return true;
            }
        }

        return false;
    }

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

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
