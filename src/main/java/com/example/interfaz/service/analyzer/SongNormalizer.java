package com.example.interfaz.service.analyzer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.interfaz.model.analyzer.ParsedTrackName;

public class SongNormalizer {

    public record ParsedMetadata(String artist, String title, Set<String> modifiers) {}

    private static final Pattern MODIFIERS_PATTERN = Pattern.compile(
            "(?i)\\b(remix|live|acoustic|instrumental|cover|radio\\s*edit|extended|demo)\\b"
    );

    private static final Pattern EXTENDED_NOISE_PATTERNS = Pattern.compile(
            "(?i)\\b(video\\s*oficial|videoclip|letra|en\\s*vivo|versi[oó]n|audio\\s*oficial|official\\s*music\\s*video|official\\s*video|official|music\\s*video|lyric\\s*video|lyrics?|audio|vevo|hd|4k|live\\s*performance|live|remastered?|remix|version|copy|copia|duplicado|new|nuevo|final|ft|feat|featuring|original|hd\\s*audio|acoustic|instrumental|cover|radio\\s*edit|extended|demo)\\b"
    );

    private static final Pattern COPY_NUMBER_PATTERNS = Pattern.compile("(?i)[_\\-\\s]*\\(\\d+\\)|[_\\-\\s]+\\d+$|[_\\-\\s]+copy\\s*\\d*|[_\\-\\s]+copia\\s*\\d*");
    private static final Pattern BRACKETS_PATTERN = Pattern.compile("[\\[\\](){}]");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    private final TitleParserService titleParser = new TitleParserService();

    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text.toLowerCase();
        int dotIdx = cleaned.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx > cleaned.length() - 6) {
            cleaned = cleaned.substring(0, dotIdx);
        }

        cleaned = COPY_NUMBER_PATTERNS.matcher(cleaned).replaceAll(" ");
        cleaned = BRACKETS_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = EXTENDED_NOISE_PATTERNS.matcher(cleaned).replaceAll(" ");
        cleaned = NON_ALPHANUMERIC_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();

        return cleaned;
    }

    public Set<String> extractModifiers(String text) {
        Set<String> modifiers = new HashSet<>();
        if (text == null || text.isBlank()) {
            return modifiers;
        }

        Matcher matcher = MODIFIERS_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            modifiers.add(matcher.group(1).trim());
        }
        return modifiers;
    }

    public ParsedMetadata parseArtistAndTitle(String fileName, String tagArtist, String tagTitle) {
        String artist = tagArtist != null ? tagArtist.trim() : "";
        String title = tagTitle != null ? tagTitle.trim() : "";

        String combinedForModifiers = (fileName != null ? fileName : "") + " " + artist + " " + title;
        Set<String> modifiers = extractModifiers(combinedForModifiers);

        ParsedTrackName parsedName = titleParser.parse(fileName);
        if (artist.isEmpty()) {
            artist = parsedName.artist();
        }
        if (title.isEmpty()) {
            title = parsedName.title();
        }

        return new ParsedMetadata(normalize(artist), normalize(title), modifiers);
    }
}
