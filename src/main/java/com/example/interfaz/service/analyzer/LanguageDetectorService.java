package com.example.interfaz.service.analyzer;

import static com.github.pemistahl.lingua.api.Language.ENGLISH;
import static com.github.pemistahl.lingua.api.Language.FRENCH;
import static com.github.pemistahl.lingua.api.Language.PORTUGUESE;
import static com.github.pemistahl.lingua.api.Language.SPANISH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.example.interfaz.model.analyzer.LanguageDetectionMethod;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.model.analyzer.LanguageInfo;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

public class LanguageDetectorService {

    public static final int DETECTOR_VERSION = 4;

    private static final LanguageDetector DETECTOR = LanguageDetectorBuilder
            .fromLanguages(SPANISH, ENGLISH, PORTUGUESE, FRENCH)
            .build();

    private static final Pattern HANGUL_PATTERN = Pattern.compile("[\\uAC00-\\uD7AF\\u1100-\\u11FF]");
    private static final Pattern KANA_PATTERN = Pattern.compile("[\\u3040-\\u30FF\\u31F0-\\u31FF]");
    private static final Pattern HAN_PATTERN = Pattern.compile("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uF900-\\uFAFF]");
    private static final Pattern MIXED_MARKER_PATTERN = Pattern.compile(
            "(?i)\\b(?:bilingual|spanglish|spanish\\s*[-/]\\s*english|english\\s*[-/]\\s*spanish)\\b");
    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile(
            "(?i)\\.(?:mp3|flac|wav|m4a|ogg|aac|wma)$");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile(
            "(?i)[\\[(](?=[^\\])]*(?:official|video|audio|lyrics?|lyric|remaster(?:ed)?|radio\\s*edit|extended\\s*mix|original\\s*mix|remix|version|versi[oó]n|hd|4k|karaoke|instrumental))[^\\])]*[\\])]");
    private static final Pattern FEATURE_PATTERN = Pattern.compile(
            "(?i)\\b(?:feat(?:uring)?|ft)\\.?\\s+.+$");
    private static final Pattern NOISE_PATTERN = Pattern.compile(
            "(?i)\\b(?:official\\s*(?:music\\s*)?(?:video|audio)|lyric\\s*video|lyrics?|remaster(?:ed)?|radio\\s*edit|extended\\s*mix|original\\s*mix|hd|4k)\\b");
    private static final Pattern COPY_PATTERN = Pattern.compile(
            "(?i)(?:\\s*[-_]\\s*)?(?:copy|copia|duplicado)(?:\\s*\\d+)?$|\\s*\\(\\d+\\)$");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final MiniLanguageModel MINI_MODEL = MiniLanguageModel.load();

    public record LanguageDetectionResult(
            String languageCode,
            String languageName,
            double confidence,
            double margin,
            LanguageDetectionMethod method,
            String alternatives
    ) {
        public LanguageInfo toLanguageInfo() {
            return new LanguageInfo(languageCode, languageName, confidence, margin,
                    method, alternatives, System.currentTimeMillis());
        }
    }

    private LanguageDetectorMode mode;

    public LanguageDetectorService() {
        this(LanguageDetectorMode.BALANCED);
    }

    public LanguageDetectorService(LanguageDetectorMode mode) {
        this.mode = mode != null ? mode : LanguageDetectorMode.BALANCED;
    }

    public LanguageDetectorMode getMode() {
        return mode;
    }

    public void setMode(LanguageDetectorMode mode) {
        this.mode = mode != null ? mode : LanguageDetectorMode.BALANCED;
    }

    public LanguageDetectionResult detectLanguage(String text) {
        return detectLanguage(text, mode);
    }

    public LanguageDetectionResult detectLanguageForTrack(String title, String artist) {
        return detectLanguageForTrack(title, artist, mode);
    }

    public LanguageDetectionResult detectLanguageForTrack(
            String title,
            String artist,
            LanguageDetectorMode targetMode
    ) {
        if (title == null || title.isBlank()) {
            return unknown();
        }
        return detectLanguage(title, targetMode);
    }

    public LanguageDetectionResult detectLanguage(String text, LanguageDetectorMode targetMode) {
        if (text == null || text.isBlank()) {
            return unknown();
        }

        if (MIXED_MARKER_PATTERN.matcher(text).find()) {
            return new LanguageDetectionResult("mixed", "Mixto / Bilingüe", 1.0, 1.0,
                    LanguageDetectionMethod.METADATA, "");
        }

        LanguageDetectionResult unicodeResult = detectUnicodeLanguage(text);
        if (unicodeResult != null) {
            return unicodeResult;
        }

        String cleanedText = cleanTitle(text);
        if (cleanedText.isBlank()) {
            return unknown();
        }

        unicodeResult = detectDistinctCharacters(cleanedText);
        if (unicodeResult != null) {
            return unicodeResult;
        }

        LanguageDetectionResult modelResult = fromModelScores(
                MINI_MODEL.predict(cleanedText), cleanedText, targetMode);
        if (modelResult != null) {
            return modelResult;
        }

        Map<Language, Double> values = DETECTOR.computeLanguageConfidenceValues(cleanedText);
        if (values.isEmpty()) {
            return ambiguous("");
        }

        List<Map.Entry<Language, Double>> ranked = new ArrayList<>(values.entrySet());
        Map.Entry<Language, Double> best = ranked.get(0);
        Map.Entry<Language, Double> second = ranked.size() > 1 ? ranked.get(1) : null;
        double secondValue = second != null ? second.getValue() : 0.0;
        double margin = Math.max(0.0, best.getValue() - secondValue);
        String bestCode = getLanguageCode(best.getKey());
        String alternatives = second != null
                ? bestCode.toUpperCase() + "/" + getLanguageCode(second.getKey()).toUpperCase()
                : bestCode.toUpperCase();

        if (margin < getRequiredMargin(cleanedText, targetMode)) {
            return new LanguageDetectionResult("ambiguous", "Ambiguo", margin, margin,
                    LanguageDetectionMethod.TEXT, alternatives);
        }

        return new LanguageDetectionResult(bestCode, getLanguageName(best.getKey()), margin, margin,
                LanguageDetectionMethod.TEXT, "");
    }

    private LanguageDetectionResult fromModelScores(double[] scores, String text, LanguageDetectorMode targetMode) {
        if (!MINI_MODEL.isLoaded()) return null;
        int best = 0;
        int second = 1;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[best]) {
                second = best;
                best = i;
            } else if (i != best && scores[i] > scores[second]) {
                second = i;
            }
        }
        double margin = Math.max(0.0, scores[best] - scores[second]);
        int words = text.split("\\s+").length;
        double requiredConfidence = words == 1 ? 0.93 : words == 2 ? 0.88 : 0.82;
        double requiredMargin = words == 1 ? 0.35 : words == 2 ? 0.25 : 0.18;
        if (targetMode == LanguageDetectorMode.PRECISE) {
            requiredConfidence += 0.02;
            requiredMargin += 0.05;
        }
        if (scores[best] < requiredConfidence || margin < requiredMargin) return null;
        String code = MiniLanguageModel.LANGUAGES[best];
        return new LanguageDetectionResult(code, languageName(code), margin, margin,
                LanguageDetectionMethod.TEXT, "");
    }

    private String languageName(String code) {
        return switch (code) {
            case "es" -> "Español";
            case "en" -> "Inglés";
            case "pt" -> "Portugués";
            case "fr" -> "Francés";
            default -> "Desconocido";
        };
    }

    String cleanTitle(String text) {
        String cleaned = FILE_EXTENSION_PATTERN.matcher(text.trim()).replaceFirst("");
        String[] parts = cleaned.split("\\s+-\\s+");
        if (parts.length > 1) {
            cleaned = parts[parts.length - 1];
        }
        cleaned = ANNOTATION_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = FEATURE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = NOISE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = COPY_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace('_', ' ');
        return MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
    }

    private LanguageDetectionResult detectUnicodeLanguage(String text) {
        if (HANGUL_PATTERN.matcher(text).find()) {
            return unicode("ko", "Coreano");
        }
        if (KANA_PATTERN.matcher(text).find()) {
            return unicode("ja", "Japonés");
        }
        if (HAN_PATTERN.matcher(text).find()) {
            return unicode("zh", "Chino");
        }
        return null;
    }

    private LanguageDetectionResult detectDistinctCharacters(String text) {
        if (text.matches(".*[ñÑ¿¡].*")) {
            return unicode("es", "Español");
        }
        if (text.matches(".*[ãõÃÕ].*")) {
            return unicode("pt", "Portugués");
        }
        if (text.matches(".*[œŒ].*")) {
            return unicode("fr", "Francés");
        }
        return null;
    }

    private LanguageDetectionResult unicode(String code, String name) {
        return new LanguageDetectionResult(code, name, 1.0, 1.0,
                LanguageDetectionMethod.UNICODE, "");
    }

    private LanguageDetectionResult unknown() {
        return new LanguageDetectionResult("unknown", "Desconocido", 0.0, 0.0,
                LanguageDetectionMethod.UNKNOWN, "");
    }

    private LanguageDetectionResult ambiguous(String alternatives) {
        return new LanguageDetectionResult("ambiguous", "Ambiguo", 0.0, 0.0,
                LanguageDetectionMethod.TEXT, alternatives);
    }

    private double getRequiredMargin(String text, LanguageDetectorMode targetMode) {
        LanguageDetectorMode activeMode = targetMode != null ? targetMode : LanguageDetectorMode.BALANCED;
        double margin = switch (activeMode) {
            case FAST -> 0.03;
            case BALANCED -> 0.12;
            case PRECISE -> 0.20;
        };
        int wordCount = text.split("\\s+").length;
        if (wordCount == 1) {
            return Math.max(margin, 0.25);
        }
        if (wordCount == 2) {
            return Math.max(margin, activeMode == LanguageDetectorMode.FAST ? 0.08 : 0.15);
        }
        return margin;
    }

    private String getLanguageCode(Language language) {
        return switch (language) {
            case SPANISH -> "es";
            case ENGLISH -> "en";
            case PORTUGUESE -> "pt";
            case FRENCH -> "fr";
            default -> "unknown";
        };
    }

    private String getLanguageName(Language language) {
        return switch (language) {
            case SPANISH -> "Español";
            case ENGLISH -> "Inglés";
            case PORTUGUESE -> "Portugués";
            case FRENCH -> "Francés";
            default -> "Desconocido";
        };
    }
}
