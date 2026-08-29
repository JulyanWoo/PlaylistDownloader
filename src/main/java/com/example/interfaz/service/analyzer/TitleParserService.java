package com.example.interfaz.service.analyzer;

import java.util.regex.Pattern;

import com.example.interfaz.model.analyzer.ParsedTrackName;

/** Extracts a title from a music filename without making language decisions. */
public class TitleParserService {
    private static final Pattern EXTENSION = Pattern.compile("(?i)\\.(?:mp3|flac|wav|m4a|ogg|aac|wma)$");
    private static final Pattern ANNOTATION = Pattern.compile(
            "(?i)\\s*[\\[(](?=[^\\])]*(?:official|video|audio|lyrics?|remaster(?:ed)?|radio\\s*edit|extended\\s*mix|original\\s*mix|remix|version|versi[oó]n|hd|4k|karaoke|instrumental))[^\\])]*[\\])]\\s*");
    private static final Pattern FEATURE = Pattern.compile("(?i)\\s+\\b(?:feat(?:uring)?|ft)\\.?\\s+.+$");
    private static final Pattern NOISE = Pattern.compile(
            "(?i)\\b(?:official\\s*(?:music\\s*)?(?:video|audio)|lyric\\s*video|lyrics?|remaster(?:ed)?|radio\\s*edit|extended\\s*mix|original\\s*mix|hd|4k)\\b");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public ParsedTrackName parse(String rawName) {
        String raw = rawName == null ? "" : rawName.trim();
        String base = EXTENSION.matcher(raw).replaceFirst("").trim();
        String artist = "";
        String title = base;
        String[] parts = base.split("\\s+-\\s+", 2);
        if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
            artist = parts[0].trim();
            title = parts[1].trim();
        }
        title = clean(title);
        return new ParsedTrackName(raw, artist, title);
    }

    public String clean(String value) {
        if (value == null) return "";
        String cleaned = EXTENSION.matcher(value.trim()).replaceFirst("");
        cleaned = ANNOTATION.matcher(cleaned).replaceAll(" ");
        cleaned = FEATURE.matcher(cleaned).replaceAll(" ");
        cleaned = NOISE.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace('_', ' ');
        return SPACES.matcher(cleaned).replaceAll(" ").trim();
    }
}
