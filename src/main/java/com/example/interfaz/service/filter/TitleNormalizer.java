package com.example.interfaz.service.filter;

public class TitleNormalizer {

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

    public static String[] normalize(String... titles) {
        String[] normalized = new String[titles.length];
        for (int i = 0; i < titles.length; i++) {
            normalized[i] = normalize(titles[i]);
        }
        return normalized;
    }

    public static boolean isNormalized(String title) {
        if (title == null) {
            return false;
        }

        String normalized = normalize(title);
        return title.equals(normalized);
    }

    public static String[] extractKeywords(String normalizedTitle) {
        if (normalizedTitle == null || normalizedTitle.trim().isEmpty()) {
            return new String[0];
        }

        return normalizedTitle.split("\\s+");
    }
}
