package com.example.interfaz.service.analyzer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SongNameNormalizer {

    private static final List<String> NOISE_WORDS = Arrays.asList(
            "official video", "official audio", "video oficial", "audio oficial",
            "lyrics", "letra", "visualizer", "remastered", "remaster",
            "live", "hd", "4k", "1080p", "feat.", "feat", "ft.", "ft"
    );

    private static final Pattern BRACKETS_AND_SYMBOLS = Pattern.compile("[()\\[\\]{}*_\\-]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    public String normalize(String inputName) {
        if (inputName == null || inputName.trim().isEmpty()) {
            return "";
        }

        String cleaned = inputName;

        // Remove extension if present
        int lastDotIndex = cleaned.lastIndexOf('.');
        if (lastDotIndex > 0) {
            cleaned = cleaned.substring(0, lastDotIndex);
        }

        cleaned = cleaned.toLowerCase();

        // Remove noise words
        for (String noise : NOISE_WORDS) {
            cleaned = cleaned.replace(noise, "");
        }

        // Remove brackets and symbols
        cleaned = BRACKETS_AND_SYMBOLS.matcher(cleaned).replaceAll(" ");

        // Remove remaining non-alphanumeric characters except spaces
        cleaned = NON_ALPHANUMERIC.matcher(cleaned).replaceAll(" ");

        // Collapse multiple spaces and trim
        cleaned = MULTIPLE_SPACES.matcher(cleaned).replaceAll(" ").trim();

        return cleaned;
    }
}
