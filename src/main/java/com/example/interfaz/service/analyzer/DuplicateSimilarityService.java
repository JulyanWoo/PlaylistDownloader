package com.example.interfaz.service.analyzer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;

public class DuplicateSimilarityService {

    private final SongNormalizer normalizer;
    private final LanguageDetectorService languageDetector;

    public DuplicateSimilarityService(SongNormalizer normalizer, LanguageDetectorService languageDetector) {
        this.normalizer = normalizer != null ? normalizer : new SongNormalizer();
        this.languageDetector = languageDetector != null ? languageDetector : new LanguageDetectorService();
    }

    public DuplicateSimilarityService() {
        this(new SongNormalizer(), new LanguageDetectorService());
    }

    public double calculateSimilarityScore(SongFile a, SongFile b) {
        return calculateSimilarityBreakdown(a, b).finalScore();
    }

    public com.example.interfaz.model.analyzer.SimilarityBreakdown calculateSimilarityBreakdown(SongFile a, SongFile b) {
        if (a == null || b == null) {
            return new com.example.interfaz.model.analyzer.SimilarityBreakdown(0, 0, 0, 0, 0, 0);
        }

        SongNormalizer.ParsedMetadata metaA = normalizer.parseArtistAndTitle(a.getFileName(), a.getArtist(), a.getTitle());
        SongNormalizer.ParsedMetadata metaB = normalizer.parseArtistAndTitle(b.getFileName(), b.getArtist(), b.getTitle());

        double titleJaro = jaroWinklerSimilarity(metaA.title(), metaB.title());
        double titleToken = calculateTokenSimilarity(metaA.title(), metaB.title());

        double artistJaro = jaroWinklerSimilarity(metaA.artist(), metaB.artist());
        double durationSim = calculateDurationSimilarity(a.getDuration(), b.getDuration());

        double score;
        if (!metaA.artist().isBlank() && !metaB.artist().isBlank()) {
            score = (titleJaro * 0.40) + (titleToken * 0.30) + (artistJaro * 0.20) + (durationSim * 0.10);
        } else {
            score = (titleJaro * 0.50) + (titleToken * 0.40) + (durationSim * 0.10);
        }

        // Language factor adjustment
        double langAdj = 0.0;
        LanguageDetectorService.LanguageDetectionResult langA = languageDetector.detectLanguage(metaA.artist() + " " + metaA.title());
        LanguageDetectorService.LanguageDetectionResult langB = languageDetector.detectLanguage(metaB.artist() + " " + metaB.title());

        if (!"unknown".equals(langA.languageCode()) && !"unknown".equals(langB.languageCode())) {
            if (!langA.languageCode().equals(langB.languageCode())) {
                langAdj = -0.05;
            } else {
                langAdj = 0.02;
            }
        }
        score += langAdj;

        // Weighted modifier penalties
        double modPenalty = 0.0;
        Set<String> modA = metaA.modifiers();
        Set<String> modB = metaB.modifiers();
        if (!modA.equals(modB)) {
            Set<String> diff = new HashSet<>(modA);
            diff.addAll(modB);
            Set<String> common = new HashSet<>(modA);
            common.retainAll(modB);
            diff.removeAll(common);

            for (String mod : diff) {
                modPenalty += getModifierPenalty(mod);
            }
        }
        score -= modPenalty;

        if (a.getDuration() > 0 && b.getDuration() > 0) {
            long diff = Math.abs(a.getDuration() - b.getDuration());
            if (diff > 30) {
                double penaltyMultiplier = Math.max(0.0, 1.0 - ((diff - 30) / 60.0));
                score *= penaltyMultiplier;
            }
        }

        double finalScore = Math.max(0.0, Math.min(1.0, score));
        return new com.example.interfaz.model.analyzer.SimilarityBreakdown(titleJaro, artistJaro, durationSim, langAdj, modPenalty, finalScore);
    }

    public double getModifierPenalty(String modifier) {
        if (modifier == null) return 0.0;
        String mod = modifier.toLowerCase();
        if (mod.contains("instrumental")) return 0.40;
        if (mod.contains("live")) return 0.25;
        if (mod.contains("remix")) return 0.20;
        if (mod.contains("acoustic")) return 0.20;
        if (mod.contains("radio")) return 0.15;
        if (mod.contains("remaster")) return 0.05;
        return 0.10;
    }

    public double calculateTokenSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isBlank() || s2.isBlank()) {
            return 0.0;
        }

        Set<String> words1 = new HashSet<>(Arrays.asList(s1.trim().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.trim().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    public double calculateDurationSimilarity(long durA, long durB) {
        if (durA <= 0 || durB <= 0) {
            return 0.5;
        }

        long diff = Math.abs(durA - durB);
        if (diff <= 5) {
            return 1.0;
        } else if (diff <= 15) {
            return 0.90;
        } else if (diff <= 30) {
            return 0.70;
        } else {
            return 0.50;
        }
    }

    public GroupClassification classifySimilarity(double score, boolean sha256Match, boolean hasModifierMismatch) {
        return classifySimilarity(score, sha256Match, hasModifierMismatch, com.example.interfaz.model.analyzer.AnalyzerProfile.BALANCED);
    }

    public GroupClassification classifySimilarity(double score, boolean sha256Match, boolean hasModifierMismatch, com.example.interfaz.model.analyzer.AnalyzerProfile profile) {
        if (sha256Match) {
            return GroupClassification.CONFIRMED_DUPLICATE;
        }
        if (hasModifierMismatch) {
            return GroupClassification.VERSION_VARIANT;
        }
        com.example.interfaz.model.analyzer.AnalyzerProfile prof = profile != null ? profile : com.example.interfaz.model.analyzer.AnalyzerProfile.BALANCED;
        if (score >= prof.getConfirmedThreshold()) {
            return GroupClassification.CONFIRMED_DUPLICATE;
        } else if (score >= prof.getPossibleThreshold()) {
            return GroupClassification.POSSIBLE_DUPLICATE;
        } else {
            return GroupClassification.SIMILAR_FILES;
        }
    }

    public double jaroWinklerSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int matchDistance = (Math.max(s1.length(), s2.length()) / 2) - 1;
        if (matchDistance < 0) matchDistance = 0;

        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];

        int matches = 0;
        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, s2.length());
            for (int j = start; j < end; j++) {
                if (s2Matches[j]) continue;
                if (s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) return 0.0;

        double t = 0.0;
        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) t += 0.5;
            k++;
        }

        double m = matches;
        double jaro = ((m / s1.length()) + (m / s2.length()) + ((m - t) / m)) / 3.0;

        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(s1.length(), s2.length())); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }

        return jaro + (prefix * 0.1 * (1.0 - jaro));
    }
}
