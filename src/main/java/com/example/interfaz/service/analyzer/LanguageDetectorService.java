package com.example.interfaz.service.analyzer;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.example.interfaz.model.analyzer.LanguageDetectorMode;

public class LanguageDetectorService {

    // Music metadata tokens that appear across all languages - NOT language indicators
    private static final Set<String> MUSIC_METADATA_TOKENS = Set.of(
            "official", "video", "audio", "lyrics", "lyric", "remix", "feat", "ft", "live",
            "acoustic", "instrumental", "version", "versao", "vivo", "acustico",
            "rock", "pop", "dance", "mp3", "flac", "wav", "m4a", "hd", "hq",
            "remastered", "radio", "edit", "single", "album", "music", "song", "track",
            "cover", "tribute", "karaoke", "extended", "mix"
    );

    public record LanguageDetectionResult(String languageCode, String languageName, double confidence) {}

    // Exclusive high-confidence stopwords (+2.0 pts)
    private static final Set<String> EXCLUSIVE_SPANISH_STOPWORDS = Set.of(
            "con", "y", "por", "para", "como", "del", "sus", "cancion", "amor", "corazon",
            "vida", "noche", "ti", "mis", "tus", "marchar", "puedes", "ahora", "resulta",
            "robarte", "darte", "beso", "besos", "ultimo", "deseo", "banda", "salsa",
            "reggaeton", "bachata", "cumbia", "corridos", "corrido", "siempre", "nunca",
            "todos", "todas", "nada", "solo", "sola", "conmigo", "contigo", "tusa",
            "recoditos", "vives", "yatra", "minaj", "royce", "hasta", "desde", "sin",
            "sobre", "cuando", "donde", "quien", "bien", "mal", "hacer", "quiero", "tengo",
            "tienes", "tiene", "nuestro", "nuestra", "chica", "chico", "bonita", "linda",
            "bello", "hermosa", "dios", "cielo", "sol", "luna", "fuego", "agua", "tierra",
            "aire", "alma", "dias", "dia", "hombre", "mujer", "sabes", "saber", "decir",
            "dime", "quedate", "quedarse", "volver", "volveras", "dame", "mira", "mirada",
            "ojos", "boca", "piel", "sueno", "suenos", "sentir", "verte", "olvidar",
            "recuerdo", "recuerdos", "lagrimas", "pensar", "vacio", "loco", "loca", "perdon",
            "otra", "otro", "nadie", "alguien"
    );

    private static final Set<String> EXCLUSIVE_ENGLISH_STOPWORDS = Set.of(
            "the", "with", "and", "from", "that", "this", "dont", "love", "you", "your",
            "what", "where", "night", "lie", "all", "about", "who", "get", "which",
            "when", "make", "like", "time", "just", "know", "take", "people", "into",
            "year", "good", "some", "could", "them", "other", "than", "then", "now",
            "look", "come", "think", "also", "back", "after", "first", "well", "way",
            "want", "because", "give", "most", "will", "would", "shall", "should",
            "girl", "boy", "heart", "baby", "world", "eyes", "life", "city", "street",
            "light", "dark", "forever", "always", "never", "together", "again", "away",
            "down", "high", "home"
    );

    private static final Set<String> EXCLUSIVE_PORTUGUESE_STOPWORDS = Set.of(
            "voce", "amor", "coracao", "nossa", "meu", "minha", "tudo", "dos", "das",
            "pelo", "pela", "tambem", "muito", "sem", "mesmo", "sobre", "ate", "quando",
            "como", "esta", "nos", "lhe", "deles", "dela", "este", "esse", "essa",
            "isto", "aquilo", "samba", "sertanejo", "funk", "saudade", "olhos", "vida", "tempo"
    );

    private static final Set<String> EXCLUSIVE_FRENCH_STOPWORDS = Set.of(
            "avec", "et", "pour", "dans", "elle", "tout", "nous", "vous", "mon",
            "mes", "ton", "ta", "tes", "son", "sa", "ses", "notre", "votre", "leur",
            "plus", "par", "moi", "toi", "lui", "chanson", "amour", "nuit", "coeur",
            "vie", "temps", "monde"
    );

    // Shared low-confidence stopwords (+0.2 pts) to prevent false positives in single-word overlaps
    private static final Set<String> SHARED_ROMANCE_STOPWORDS = Set.of(
            "de", "la", "que", "el", "en", "a", "los", "las", "se", "un", "una", "unos", "unas",
            "al", "lo", "mas", "pero", "le", "ya", "o", "du", "des", "au", "est", "su", "sus", "tu", "mi"
    );

    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff\\uac00-\\ud7af]");
    
    private static final Pattern MUSIC_ANNOTATION_PATTERN = Pattern.compile(
            "(?i)\\((?:official\\s*(?:video|audio|lyric|music|clip)|lyric\\s*video|hd|hq|4k|music\\s*video|" +
            "video\\s*oficial|audio\\s*oficial|letra|karaoke|instrumental|remastered|radio\\s*edit|" +
            "extended\\s*mix|version\\s*\\d{4}|\\d{4}|ver\\.?\\s*\\d{2})\\)|" +
            "\\bfeat\\.?\\s+[\\w\\s,&]+|\\bft\\.?\\s+[\\w\\s,&]+|" +
            "\\.(mp3|flac|wav|m4a|ogg|aac|wma)$"
    );

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
        return detectLanguage(text, this.mode);
    }

    public LanguageDetectionResult detectLanguageForTrack(String title, String artist) {
        return detectLanguageForTrack(title, artist, this.mode);
    }

    public LanguageDetectionResult detectLanguageForTrack(String title, String artist, LanguageDetectorMode targetMode) {
        if ((artist == null || artist.isBlank()) && (title == null || title.isBlank())) {
            return new LanguageDetectionResult("unknown", "Desconocido", 0.0);
        }

        if (artist == null || artist.isBlank()) {
            return detectLanguage(title, targetMode);
        }
        if (title == null || title.isBlank()) {
            return detectLanguage(artist, targetMode);
        }

        // Apply 85% weight to Title analysis and 15% weight to Artist analysis
        LanguageDetectionResult titleResult = detectLanguage(title, targetMode);
        LanguageDetectionResult artistResult = detectLanguage(artist, targetMode);

        if (!titleResult.languageCode().equals("unknown") && !titleResult.languageCode().equals("mixed")) {
            if (artistResult.languageCode().equals(titleResult.languageCode())) {
                double combinedConf = Math.min(1.0, (titleResult.confidence() * 0.85) + (artistResult.confidence() * 0.15) + 0.05);
                return new LanguageDetectionResult(titleResult.languageCode(), titleResult.languageName(), combinedConf);
            }
            return new LanguageDetectionResult(titleResult.languageCode(), titleResult.languageName(), titleResult.confidence() * 0.90);
        }

        return titleResult.confidence() >= artistResult.confidence() ? titleResult : artistResult;
    }

    public LanguageDetectionResult detectLanguage(String text, LanguageDetectorMode targetMode) {
        LanguageDetectorMode activeMode = targetMode != null ? targetMode : LanguageDetectorMode.BALANCED;

        if (text == null || text.isBlank()) {
            return new LanguageDetectionResult("unknown", "Desconocido", 0.0);
        }

        String lowerText = text.toLowerCase();
        if (lowerText.contains("english version") || lowerText.contains("spanish version") || lowerText.contains("bilingual") || lowerText.contains("spanglish")) {
            return new LanguageDetectionResult("mixed", "Mixto / Bilingüe", 0.85);
        }

        if (CJK_PATTERN.matcher(text).find()) {
            return new LanguageDetectionResult("ja_cjk", "Asiático (CJK)", 0.95);
        }

        // Strategy 2: If single string contains '-' e.g., "Artist - Title.mp3", extract right side as Title
        String targetText = text;
        if (text.contains("-")) {
            String[] parts = text.split("-");
            if (parts.length >= 2) {
                targetText = parts[parts.length - 1].trim(); // Extract title on the right
            }
        }

        // Strategy 1: Pre-filter distinct language diacritics BEFORE NFD normalization
        double esScore = 0.0;
        double enScore = 0.0;
        double ptScore = 0.0;
        double frScore = 0.0;

        for (char c : targetText.toCharArray()) {
            switch (c) {
                case 'ñ', 'Ñ', '¿', '¡' -> esScore += 3.0;
                case 'ç', 'Ç', 'à', 'è', 'ù', 'œ', 'Œ' -> frScore += 3.0;
                case 'ã', 'õ', 'Ã', 'Õ' -> ptScore += 3.0;
                default -> {}
            }
        }

        // Strip music metadata annotations before word tokenization
        String stripped = MUSIC_ANNOTATION_PATTERN.matcher(targetText.toLowerCase()).replaceAll(" ");

        // Normalize unicode accents e.g., 'á' -> 'a'
        String normalized = Normalizer.normalize(stripped, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String cleaned = normalized.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        String[] words = cleaned.split("\\s+");

        if (words.length == 0 && esScore == 0 && frScore == 0 && ptScore == 0) {
            return new LanguageDetectionResult("unknown", "Desconocido", 0.0);
        }

        // Strategy 4 & 5: Exclusive vs Shared Stopwords + N-Grams
        for (String w : words) {
            if (MUSIC_METADATA_TOKENS.contains(w)) continue;

            // Exclusive Stopwords (+2.0)
            if (EXCLUSIVE_SPANISH_STOPWORDS.contains(w)) esScore += 2.0;
            if (EXCLUSIVE_ENGLISH_STOPWORDS.contains(w)) enScore += 2.0;
            if (EXCLUSIVE_PORTUGUESE_STOPWORDS.contains(w)) ptScore += 2.0;
            if (EXCLUSIVE_FRENCH_STOPWORDS.contains(w)) frScore += 2.0;

            // Shared Low-Confidence Stopwords (+0.2)
            if (SHARED_ROMANCE_STOPWORDS.contains(w)) {
                esScore += 0.2;
                frScore += 0.2;
                ptScore += 0.2;
            }

            // Strategy 5: Distinct N-Grams (+1.5)
            if (w.contains("th") || w.contains("gh") || w.contains("wh") || w.contains("ck") || w.contains("oo")) {
                enScore += 1.5;
            }
            if (w.contains("ll") || w.contains("qu") || w.contains("ñ")) {
                esScore += 1.5;
            }
            if (w.contains("ou") || w.contains("eau") || w.contains("eux")) {
                frScore += 1.5;
            }
        }

        if (esScore >= 3.0 && enScore >= 3.0 && activeMode != LanguageDetectorMode.CONSERVATIVE) {
            return new LanguageDetectionResult("mixed", "Mixto / Bilingüe", 0.80);
        }

        Map<String, Double> scores = new HashMap<>();
        scores.put("es", esScore);
        scores.put("en", enScore);
        scores.put("pt", ptScore);
        scores.put("fr", frScore);

        String bestLang = "es";
        double maxScore = 0.0;

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestLang = entry.getKey();
            }
        }

        if (maxScore == 0.0) {
            if (activeMode == LanguageDetectorMode.CONSERVATIVE) {
                return new LanguageDetectionResult("unknown", "Ambiguo", 0.30);
            }

            // Strategy 3: Enhanced Morphological Endings Heuristic
            LanguageDetectionResult heuristic = detectByHeuristics(words, activeMode);
            if (heuristic != null) {
                return heuristic;
            }

            return new LanguageDetectionResult("unknown", "Ambiguo", 0.30);
        }

        double confidence = switch (activeMode) {
            case AGGRESSIVE -> Math.min(1.0, 0.65 + (maxScore * 0.10));
            case CONSERVATIVE -> Math.min(1.0, 0.40 + (maxScore * 0.10));
            default -> Math.min(1.0, 0.50 + (maxScore * 0.10));
        };

        String langName = getLanguageName(bestLang);
        return new LanguageDetectionResult(bestLang, langName, confidence);
    }

    private LanguageDetectionResult detectByHeuristics(String[] words, LanguageDetectorMode mode) {
        int esHeuristicScore = 0;
        int enHeuristicScore = 0;

        for (String w : words) {
            if (w.length() < 3) continue;

            // Strategy 3: Spanish morphological endings & spanish end consonants (a, e, i, o, u, d, l, r, n, s, z)
            if (w.endsWith("cion") || w.endsWith("sion") || w.endsWith("ando") || w.endsWith("endo") ||
                w.endsWith("arte") || w.endsWith("erte") || w.endsWith("oso") || w.endsWith("osa") ||
                w.endsWith("itas") || w.endsWith("itos") || w.endsWith("illa") || w.endsWith("illo") ||
                w.endsWith("miento") || w.endsWith("amente")) {
                esHeuristicScore += 2;
            } else {
                char lastChar = w.charAt(w.length() - 1);
                if (lastChar == 'a' || lastChar == 'e' || lastChar == 'i' || lastChar == 'o' || lastChar == 'u' ||
                    lastChar == 'd' || lastChar == 'l' || lastChar == 'r' || lastChar == 'n' || lastChar == 's' || lastChar == 'z') {
                    esHeuristicScore += 1;
                }
            }

            // Strategy 3: English morphological endings & hard consonants (t, k, g, w, y, h, p)
            if (w.endsWith("ing") || w.endsWith("tion") || w.endsWith("ed") || w.endsWith("less") ||
                w.endsWith("ness") || w.endsWith("ight") || w.endsWith("ly") || w.endsWith("ty")) {
                enHeuristicScore += 2;
            } else {
                char lastChar = w.charAt(w.length() - 1);
                if (lastChar == 't' || lastChar == 'k' || lastChar == 'g' || lastChar == 'w' ||
                    lastChar == 'y' || lastChar == 'h' || lastChar == 'p') {
                    enHeuristicScore += 1;
                }
            }
        }

        if (esHeuristicScore > enHeuristicScore) {
            double conf = mode == LanguageDetectorMode.AGGRESSIVE ? 0.70 : 0.55;
            return new LanguageDetectionResult("es", "Español", conf);
        } else if (enHeuristicScore > esHeuristicScore) {
            double conf = mode == LanguageDetectorMode.AGGRESSIVE ? 0.70 : 0.55;
            return new LanguageDetectionResult("en", "Inglés", conf);
        }

        return null;
    }

    private String getLanguageName(String code) {
        return switch (code) {
            case "es" -> "Español";
            case "en" -> "Inglés";
            case "pt" -> "Portugués";
            case "fr" -> "Francés";
            default -> "Inglés";
        };
    }
}
