package com.example.interfaz.service.analyzer;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.example.interfaz.model.analyzer.ParsedTrackName;

/**
 * Extracts artist and title data from common music filename formats without
 * making language decisions.
 */
public class TitleParserService {
    private static final Pattern EXTENSION = Pattern.compile("(?iu)\\.(?:mp3|flac|wav|m4a|ogg|aac|wma)$");
    private static final Pattern TRACK_NUMBER = Pattern.compile("^\\s*\\d{1,3}\\s*[._)\\-]+\\s*");
    private static final Pattern ANNOTATION = Pattern.compile(
            "(?iu)\\s*[\\[(](?=[^\\])]*(?:official|video|audio|lyrics?|letras?|remaster(?:ed)?|radio\\s*edit|extended\\s*mix|original\\s*mix|remix|version|versi[oó]n|hd|4k|karaoke|instrumental))[^\\])]*[\\])]\\s*");
    private static final Pattern FEATURE = Pattern.compile("(?iu)\\s+\\b(?:feat(?:uring)?|ft)\\.?\\s+.+$");
    private static final Pattern NOISE = Pattern.compile(
            "(?iu)\\b(?:official\\s*(?:music\\s*)?(?:video|audio)|video\\s*oficial|audio\\s*oficial|video\\s*lyric|lyric\\s*video|lyrics?|letras?|remaster(?:ed)?|radio\\s*edit|extended\\s*mix|original\\s*mix|hd|4k)\\b");
    private static final Pattern TRAILING_UPLOAD_LABEL = Pattern.compile(
            "(?iu)\\s*(?:[|｜]\\s*|[-–—]\\s*)(?:official\\s*(?:music\\s*)?(?:video|audio)|video\\s*oficial|audio\\s*oficial|video\\s*lyric|lyric\\s*video|lyrics?|letras?|visualizer|videoclip|vevo|hd|4k)\\b.*$");
    private static final Pattern TRAILING_SEPARATORS = Pattern.compile("(?:\\s*(?:[|｜]|[-–—])\\s*)+$");
    private static final Pattern DASH_SEPARATOR = Pattern.compile("\\s+[-–—]\\s+");
    private static final Pattern ARTIST_JOINER = Pattern.compile(
            "(?iu)(?:\\s+\\b(?:x|feat(?:uring)?|ft)\\.?\\s+|\\s*&\\s*|,)");
    private static final Pattern TITLE_VERSION = Pattern.compile(
            "(?iu)\\b(?:en\\s+vivo|live|remix|ac[uú]stic[ao]|version|versi[oó]n)\\b");
    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    /*
     * These terms only help establish which side of "A - B" is most likely
     * a song title. They are deliberately generic and not artist names.
     */
    private static final Set<String> TITLE_CUES = Set.of(
            "a", "al", "adios", "amigo", "amor", "asi", "aqui", "au", "avec", "aunque",
            "bandida", "bandido", "beso", "besitos", "borracho", "campeon", "cancion",
            "carretera", "con", "contigo", "corazon", "coracao", "cuando", "quando",
            "de", "del", "dejala", "dejame", "despedida", "desquite", "destino", "do",
            "dos", "du", "el", "ella", "en", "es", "esta", "este", "eu", "for", "forever",
            "je", "la", "las", "le", "les", "lo", "los", "mala", "me", "meu", "minha",
            "mi", "mis", "mon", "mujer", "mujeres", "muy", "nada", "no", "non", "nao",
            "noche", "o", "para", "perdon", "por", "que", "quero", "quiero", "se", "si",
            "sin", "solo", "te", "the", "ti", "to", "todo", "tu", "una", "un", "une",
            "vida", "voy", "we", "with", "ya", "yo", "you"
    );

    /*
     * A small generic collection of given names helps recognize a multi-word
     * performer name without tying the parser to this user's library.
     */
    private static final Set<String> COMMON_GIVEN_NAMES = Set.of(
            "adrian", "alberto", "alejandro", "alex", "alfonso", "andres", "antonio",
            "carlos", "carin", "dario", "david", "diego", "eduardo", "emilio", "felipe",
            "gabriel", "georgy", "hebert", "hector", "jean", "jessi", "jesus", "jhon",
            "john", "jhonny", "jose", "juan", "julian", "luis", "manuel", "maria", "miguel",
            "oscar", "pablo", "pedro", "pipe", "rafael", "ricardo", "roberto", "santiago",
            "sebastian", "yeison"
    );

    public ParsedTrackName parse(String rawName) {
        return parse(rawName, Set.of());
    }

    /**
     * Uses performer names observed elsewhere in the same library to resolve
     * otherwise indistinguishable one-word titles and artists.
     */
    public ParsedTrackName parse(String rawName, Set<String> knownArtistKeys) {
        String raw = rawName == null ? "" : rawName.trim();
        String base = clean(raw);
        if (base.isBlank()) {
            return new ParsedTrackName(raw, "", "");
        }

        String[] parts = DASH_SEPARATOR.split(base, 2);
        if (parts.length != 2) {
            return new ParsedTrackName(raw, "", base);
        }

        String left = clean(parts[0]);
        String right = clean(parts[1]);
        if (left.isBlank() || right.isBlank()) {
            return new ParsedTrackName(raw, "", left.isBlank() ? right : left);
        }

        return switch (chooseDirection(left, right, knownArtistKeys)) {
            case ARTIST_FIRST -> new ParsedTrackName(raw, left, right);
            case TITLE_FIRST -> new ParsedTrackName(raw, right, left);
            case UNKNOWN -> new ParsedTrackName(raw, "", left + " - " + right);
        };
    }

    public String clean(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = EXTENSION.matcher(value.trim()).replaceFirst("");
        cleaned = TRAILING_UPLOAD_LABEL.matcher(cleaned).replaceFirst("");
        cleaned = ANNOTATION.matcher(cleaned).replaceAll(" ");
        cleaned = FEATURE.matcher(cleaned).replaceAll(" ");
        cleaned = NOISE.matcher(cleaned).replaceAll(" ");
        cleaned = TRACK_NUMBER.matcher(cleaned).replaceFirst("");
        cleaned = TRAILING_SEPARATORS.matcher(cleaned).replaceFirst("");
        cleaned = cleaned.replace('_', ' ');
        return SPACES.matcher(cleaned).replaceAll(" ").trim();
    }

    /**
     * Produces a stable, accent-insensitive key suitable for comparing artists.
     */
    public String artistKey(String value) {
        return WORD_SEPARATOR.matcher(comparable(value)).replaceAll(" ").trim();
    }

    private Direction chooseDirection(String left, String right, Set<String> knownArtistKeys) {
        Set<String> artists = knownArtistKeys == null ? Set.of() : knownArtistKeys;
        boolean leftIsKnownArtist = artists.contains(artistKey(left));
        boolean rightIsKnownArtist = artists.contains(artistKey(right));
        if (leftIsKnownArtist && !rightIsKnownArtist) {
            return Direction.ARTIST_FIRST;
        }
        if (rightIsKnownArtist && !leftIsKnownArtist) {
            return Direction.TITLE_FIRST;
        }

        int artistFirstScore = artistEvidence(left) + titleEvidence(right);
        int titleFirstScore = artistEvidence(right) + titleEvidence(left);

        if (artistFirstScore > titleFirstScore) {
            return Direction.ARTIST_FIRST;
        }
        if (titleFirstScore > artistFirstScore) {
            return Direction.TITLE_FIRST;
        }
        return Direction.UNKNOWN;
    }

    private int artistEvidence(String value) {
        String comparable = comparable(value);
        String[] words = words(comparable);
        int titleEvidence = titleEvidence(value);
        int score = 0;

        if (ARTIST_JOINER.matcher(value).find()) {
            score += 4;
        }

        int givenNames = 0;
        for (String word : words) {
            if (COMMON_GIVEN_NAMES.contains(word)) {
                givenNames++;
            }
        }
        if (givenNames >= 2) {
            score += 4;
        } else if (givenNames == 1 && words.length >= 2) {
            score += 3;
        }

        if (titleEvidence == 0) {
            int titleCaseWords = countTitleCaseWords(value);
            if (words.length >= 2 && (titleCaseWords >= 2 || isAllUpperCase(value))) {
                score++;
            } else if (words.length == 1 && (titleCaseWords == 1 || isAllUpperCase(value))) {
                score++;
            }
        }

        return score;
    }

    private int titleEvidence(String value) {
        int score = 0;
        for (String word : words(comparable(value))) {
            if (TITLE_CUES.contains(word)) {
                score++;
            }
        }
        if (TITLE_VERSION.matcher(value).find()) {
            score++;
        }
        return Math.min(score, 4);
    }

    private String comparable(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(normalized)
                .replaceAll("")
                .toLowerCase(Locale.ROOT);
    }

    private String[] words(String value) {
        String trimmed = WORD_SEPARATOR.matcher(value).replaceAll(" ").trim();
        return trimmed.isBlank() ? new String[0] : trimmed.split("\\s+");
    }

    private int countTitleCaseWords(String value) {
        int count = 0;
        for (String word : words(value)) {
            if (!word.isEmpty() && Character.isUpperCase(word.codePointAt(0))) {
                count++;
            }
        }
        return count;
    }

    private boolean isAllUpperCase(String value) {
        boolean hasLetter = false;
        for (int index = 0; index < value.length();) {
            int codePoint = value.codePointAt(index);
            if (Character.isLetter(codePoint)) {
                hasLetter = true;
                if (Character.isLowerCase(codePoint)) {
                    return false;
                }
            }
            index += Character.charCount(codePoint);
        }
        return hasLetter;
    }

    private enum Direction {
        ARTIST_FIRST,
        TITLE_FIRST,
        UNKNOWN
    }
}
