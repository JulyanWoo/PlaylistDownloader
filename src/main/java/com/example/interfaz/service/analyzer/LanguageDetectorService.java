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

    private static final Set<String> SPANISH_STOPWORDS = Set.of(
            "de", "la", "que", "el", "en", "y", "a", "los", "las", "del", "se", "por", "un", "una", "unos", "unas",
            "para", "con", "no", "su", "sus", "al", "lo", "como", "mas", "pero", "le", "ya", "o", "este", "esta", "esto",
            "estos", "estas", "amor", "corazon", "vida", "noche", "ti", "mi", "mis", "tu", "tus", "marchar", "te", "puedes",
            "ahora", "resulta", "robarte", "darte", "beso", "besos", "ultimo", "deseo", "cancion", "copia",
            "banda", "salsa", "reggaeton", "bachata", "cumbia", "corridos", "corrido", "siempre", "nunca", "todo",
            "todos", "todas", "nada", "solo", "sola", "conmigo", "contigo", "tusa", "recoditos", "vives", "yatra", "minaj",
            "royce", "hasta", "desde", "sin", "sobre", "cuando", "donde", "quien", "bien", "mal", "hacer", "quiero", "tengo",
            "tienes", "tiene", "nuestro", "nuestra", "chica", "chico", "bonita", "linda", "bello", "hermosa", "dios", "cielo",
            "sol", "luna", "fuego", "agua", "tierra", "aire", "alma", "dias", "dia", "hombre", "hombres", "mujer", "mujeres",
            "sabes", "saber", "decir", "dime", "quedate", "quedarse", "volver", "volveras", "dame", "mira", "mirada", "ojos",
            "boca", "piel", "sueno", "suenos", "sentir", "verte", "olvidar", "recuerdo", "recuerdos", "lagrimas", "pensar",
            "vacio", "loco", "loca", "perdon", "otra", "otro", "nadie", "alguien"
    );

    private static final Set<String> ENGLISH_STOPWORDS = Set.of(
            "the", "be", "to", "of", "and", "in", "that", "have", "it", "for", "not", "on", "with", "he",
            "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "love",
            "night", "me", "dont", "lie", "my", "your", "all", "so", "up", "out", "if", "about", "who", "get", "which",
            "go", "when", "make", "can", "like", "time", "just", "him", "know", "take", "people", "into", "year",
            "good", "some", "could", "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over",
            "think", "also", "back", "after", "use", "two", "how", "our", "first", "well", "way", "even", "new",
            "want", "because", "any", "these", "give", "day", "most", "us", "is", "are", "was", "were", "been", "being",
            "has", "had", "does", "did", "done", "will", "would", "shall", "should", "may", "might", "must",
            "girl", "boy", "heart", "baby", "world", "eyes", "life", "city", "street", "light", "dark", "forever", "always",
            "never", "together", "again", "away", "down", "high", "home"
    );

    private static final Set<String> PORTUGUESE_STOPWORDS = Set.of(
            "do", "da", "que", "um", "para", "com", "nao", "uma", "no", "se", "na", "por", "mais", "voce", "amor",
            "coracao", "nossa", "meu", "minha", "tudo", "ao", "dos", "das", "pelo", "pela", "tambem", "muito", "sem",
            "mesmo", "sobre", "ate", "quando", "como", "esta", "nos", "lhe", "deles", "dela", "este", "esse", "essa",
            "isto", "aquilo", "samba", "sertanejo", "funk", "saudade", "olhos", "vida", "tempo"
    );

    private static final Set<String> FRENCH_STOPWORDS = Set.of(
            "le", "la", "les", "de", "un", "une", "a", "etre", "et", "en", "avoir", "que", "pour", "dans", "ce",
            "il", "qui", "ne", "sur", "se", "pas", "du", "elle", "au", "des", "avec", "tout", "nous", "vous", "mon",
            "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses", "notre", "votre", "leur", "plus", "par", "je",
            "tu", "on", "moi", "toi", "lui", "chanson", "amour", "nuit", "coeur", "vie", "temps", "monde"
    );

    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff\\uac00-\\ud7af]");
    /** Strip common music metadata annotations from text before language analysis */
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

        // Strip music metadata annotations before analysis to avoid false positives
        String stripped = MUSIC_ANNOTATION_PATTERN.matcher(lowerText).replaceAll(" ");

        // Normalize unicode accents e.g., 'á' -> 'a'
        String normalized = Normalizer.normalize(stripped, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String cleaned = normalized.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        String[] words = cleaned.split("\\s+");

        if (words.length == 0) {
            return new LanguageDetectionResult("unknown", "Desconocido", 0.0);
        }

        int esCount = 0;
        int enCount = 0;
        int ptCount = 0;
        int frCount = 0;

        for (String w : words) {
            if (MUSIC_METADATA_TOKENS.contains(w)) continue; // Skip language-neutral music tokens
            if (SPANISH_STOPWORDS.contains(w)) esCount++;
            if (ENGLISH_STOPWORDS.contains(w)) enCount++;
            if (PORTUGUESE_STOPWORDS.contains(w)) ptCount++;
            if (FRENCH_STOPWORDS.contains(w)) frCount++;
        }

        // Require at least 2 hits in EACH language before declaring "mixed" to avoid false positives
        if (esCount >= 2 && enCount >= 2 && activeMode != LanguageDetectorMode.CONSERVATIVE) {
            return new LanguageDetectionResult("mixed", "Mixto / Bilingüe", 0.80);
        }

        Map<String, Integer> scores = new HashMap<>();
        scores.put("es", esCount);
        scores.put("en", enCount);
        scores.put("pt", ptCount);
        scores.put("fr", frCount);

        String bestLang = "es";
        int maxScore = 0;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestLang = entry.getKey();
            }
        }

        if (maxScore == 0) {
            if (activeMode == LanguageDetectorMode.CONSERVATIVE) {
                return new LanguageDetectionResult("unknown", "Ambiguo", 0.30);
            }

            // Heuristic detection for BALANCED / AGGRESSIVE modes
            LanguageDetectionResult heuristic = detectByHeuristics(words, activeMode);
            if (heuristic != null) {
                return heuristic;
            }

            return new LanguageDetectionResult("unknown", "Ambiguo", 0.30);
        }

        double confidence = switch (activeMode) {
            case AGGRESSIVE -> Math.min(1.0, 0.65 + (maxScore * 0.15));
            case CONSERVATIVE -> Math.min(1.0, 0.40 + (maxScore * 0.15));
            default -> Math.min(1.0, 0.50 + (maxScore * 0.15));
        };

        String langName = getLanguageName(bestLang);
        return new LanguageDetectionResult(bestLang, langName, confidence);
    }

    private LanguageDetectionResult detectByHeuristics(String[] words, LanguageDetectorMode mode) {
        int esHeuristicScore = 0;
        int enHeuristicScore = 0;

        for (String w : words) {
            if (w.length() < 3) continue;

            // Common Spanish endings & letter patterns
            if (w.endsWith("cion") || w.endsWith("sion") || w.endsWith("ando") || w.endsWith("endo") ||
                w.endsWith("arte") || w.endsWith("erte") || w.endsWith("oso") || w.endsWith("osa") ||
                w.endsWith("itas") || w.endsWith("itos") || w.endsWith("illa") || w.endsWith("illo") ||
                w.endsWith("miento") || w.endsWith("amente")) {
                esHeuristicScore += 2;
            } else if (w.endsWith("ar") || w.endsWith("er") || w.endsWith("ir") || w.endsWith("es") || w.endsWith("os")) {
                esHeuristicScore += 1;
            }

            // Common English endings & letter patterns
            if (w.endsWith("ing") || w.endsWith("tion") || w.endsWith("ed") || w.endsWith("less") ||
                w.endsWith("ness") || w.contains("th") || w.contains("sh") || w.contains("wh") ||
                w.endsWith("ight") || w.contains("ck") || w.contains("oo") || w.contains("ea")) {
                enHeuristicScore += 2;
            } else if (w.endsWith("er") || w.endsWith("est") || w.endsWith("ly")) {
                enHeuristicScore += 1;
            }
        }

        int max = Math.max(esHeuristicScore, enHeuristicScore);
        if (max == 0) {
            if (mode == LanguageDetectorMode.AGGRESSIVE) {
                // In aggressive mode, check overall vowel endings (common in Spanish)
                int esVowelEnds = 0;
                int enConsonantEnds = 0;
                for (String w : words) {
                    if (w.length() >= 3) {
                        char lastChar = w.charAt(w.length() - 1);
                        if (lastChar == 'a' || lastChar == 'e' || lastChar == 'o') {
                            esVowelEnds++;
                        } else if (lastChar == 'k' || lastChar == 't' || lastChar == 'g' || lastChar == 'd' || lastChar == 'y') {
                            enConsonantEnds++;
                        }
                    }
                }
                if (esVowelEnds > enConsonantEnds) {
                    return new LanguageDetectionResult("es", "Español", 0.60);
                } else if (enConsonantEnds > esVowelEnds) {
                    return new LanguageDetectionResult("en", "Inglés", 0.60);
                }
            }
            return null;
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
