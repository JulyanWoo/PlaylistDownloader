package com.example.interfaz.service.analyzer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.analyzer.strategy.OriginalSelectionStrategy;
import com.example.interfaz.service.analyzer.strategy.QualityFirstStrategy;

public class DuplicateDetectionService {

    private static final double SIMILARITY_THRESHOLD = 0.65;

    private final DuplicateSimilarityService similarityService;
    private final SongNormalizer normalizer;
    private final ExactHashDetector hashDetector;
    private final OriginalSelectionStrategy selectionStrategy;

    public DuplicateDetectionService(DuplicateSimilarityService similarityService,
                                      SongNormalizer normalizer,
                                      ExactHashDetector hashDetector,
                                      OriginalSelectionStrategy selectionStrategy) {
        this.similarityService = similarityService != null ? similarityService : new DuplicateSimilarityService();
        this.normalizer = normalizer != null ? normalizer : new SongNormalizer();
        this.hashDetector = hashDetector != null ? hashDetector : new ExactHashDetector();
        this.selectionStrategy = selectionStrategy != null ? selectionStrategy : new QualityFirstStrategy();
    }

    public DuplicateDetectionService() {
        this(new DuplicateSimilarityService(), new SongNormalizer(), new ExactHashDetector(), new QualityFirstStrategy());
    }

    public List<DuplicateGroup> detectDuplicates(List<SongFile> songs) {
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        if (songs == null || songs.isEmpty()) {
            return duplicateGroups;
        }

        Map<String, List<SongFile>> buckets = createFastBuckets(songs);
        boolean[] processedGlobal = new boolean[songs.size()];

        for (List<SongFile> bucket : buckets.values()) {
            if (bucket.size() < 2) continue;

            boolean[] processedInBucket = new boolean[bucket.size()];

            for (int i = 0; i < bucket.size(); i++) {
                if (processedInBucket[i]) continue;

                SongFile base = bucket.get(i);
                int baseGlobalIndex = songs.indexOf(base);
                if (baseGlobalIndex >= 0 && processedGlobal[baseGlobalIndex]) continue;

                List<SongFile> cluster = new ArrayList<>();
                cluster.add(base);

                double maxScoreInCluster = 0.0;
                boolean clusterSha256Match = false;
                boolean hasModifierMismatchInCluster = false;

                SongNormalizer.ParsedMetadata metaBase = normalizer.parseArtistAndTitle(base.getFileName(), base.getArtist(), base.getTitle());

                for (int j = i + 1; j < bucket.size(); j++) {
                    if (processedInBucket[j]) continue;

                    SongFile candidate = bucket.get(j);
                    int candGlobalIndex = songs.indexOf(candidate);
                    if (candGlobalIndex >= 0 && processedGlobal[candGlobalIndex]) continue;

                    boolean sha256Match = hashDetector.isExactMatch(base, candidate);
                    double score = sha256Match ? 1.0 : similarityService.calculateSimilarityScore(base, candidate);

                    SongNormalizer.ParsedMetadata metaCand = normalizer.parseArtistAndTitle(candidate.getFileName(), candidate.getArtist(), candidate.getTitle());
                    boolean modMismatch = !metaBase.modifiers().equals(metaCand.modifiers());

                    if (sha256Match || score >= SIMILARITY_THRESHOLD) {
                        cluster.add(candidate);
                        processedInBucket[j] = true;
                        if (candGlobalIndex >= 0) processedGlobal[candGlobalIndex] = true;

                        if (score > maxScoreInCluster) {
                            maxScoreInCluster = score;
                        }
                        if (sha256Match) {
                            clusterSha256Match = true;
                        }
                        if (modMismatch) {
                            hasModifierMismatchInCluster = true;
                        }
                    }
                }

                if (cluster.size() > 1) {
                    processedInBucket[i] = true;
                    if (baseGlobalIndex >= 0) processedGlobal[baseGlobalIndex] = true;

                    GroupClassification classification = similarityService.classifySimilarity(maxScoreInCluster, clusterSha256Match, hasModifierMismatchInCluster);

                    List<DuplicateCandidate> candidates = createCandidates(cluster);
                    String groupTitle = base.getTitle() != null && !base.getTitle().isBlank()
                            ? base.getTitle()
                            : base.getFileName();

                    double groupConfidence = clusterSha256Match ? 1.0 : maxScoreInCluster;
                    duplicateGroups.add(new DuplicateGroup(groupTitle, candidates, classification, groupConfidence));
                }
            }
        }

        return duplicateGroups;
    }

    private Map<String, List<SongFile>> createFastBuckets(List<SongFile> songs) {
        Map<String, List<SongFile>> buckets = new HashMap<>();

        for (SongFile song : songs) {
            SongNormalizer.ParsedMetadata meta = normalizer.parseArtistAndTitle(song.getFileName(), song.getArtist(), song.getTitle());

            List<String> keys = new ArrayList<>();

            if (!meta.title().isBlank()) {
                String[] tokens = meta.title().split("\\s+");
                for (String t : tokens) {
                    if (t.length() > 2) {
                        keys.add("t_" + t);
                    }
                }
            }

            if (!meta.artist().isBlank()) {
                String[] tokens = meta.artist().split("\\s+");
                for (String t : tokens) {
                    if (t.length() > 2) {
                        keys.add("a_" + t);
                    }
                }
            }

            if (song.getDuration() > 0) {
                keys.add("dur_" + (song.getDuration() / 15));
            }

            if (keys.isEmpty()) {
                keys.add("generic");
            }

            for (String key : keys) {
                List<SongFile> bucket = buckets.computeIfAbsent(key, k -> new ArrayList<>());
                if (!bucket.contains(song)) {
                    bucket.add(song);
                }
            }
        }

        return buckets;
    }

    public String computeSha256(Path path) {
        return hashDetector.computeSha256(path);
    }

    private LanguageDetectorMode languageDetectorMode = LanguageDetectorMode.BALANCED;

    public LanguageDetectorMode getLanguageDetectorMode() {
        return languageDetectorMode;
    }

    public void setLanguageDetectorMode(LanguageDetectorMode mode) {
        this.languageDetectorMode = mode != null ? mode : LanguageDetectorMode.BALANCED;
    }

    private List<DuplicateCandidate> createCandidates(List<SongFile> songs) {
        List<DuplicateCandidate> candidates = new ArrayList<>();

        if (songs == null || songs.isEmpty()) {
            return candidates;
        }

        SongFile bestFile = songs.get(0);
        OriginalScoreCalculator.QualityEvaluation bestEval = selectionStrategy.evaluate(bestFile);

        for (SongFile s : songs) {
            OriginalScoreCalculator.QualityEvaluation eval = selectionStrategy.evaluate(s);
            if (eval.score() > bestEval.score()) {
                bestEval = eval;
                bestFile = s;
            }
        }

        LanguageDetectorService langDetector = new LanguageDetectorService(languageDetectorMode);

        for (SongFile s : songs) {
            boolean isOriginal = (s == bestFile);
            com.example.interfaz.model.analyzer.SimilarityBreakdown breakdown = isOriginal
                    ? new com.example.interfaz.model.analyzer.SimilarityBreakdown(1.0, 1.0, 1.0, 0.0, 0.0, 1.0)
                    : similarityService.calculateSimilarityBreakdown(bestFile, s);

            double score = breakdown.finalScore();
            OriginalScoreCalculator.QualityEvaluation eval = selectionStrategy.evaluate(s);

            DuplicateCandidate candidate = new DuplicateCandidate(s, !isOriginal, isOriginal, score);
            candidate.setOriginalScore((int) Math.round(eval.score()));
            candidate.setScoreReasons(eval.reasons());
            candidate.setSimilarityBreakdown(breakdown);

            com.example.interfaz.model.analyzer.LanguageInfo languageInfo = s.getLanguageInfo();
            if (languageInfo == null || "unknown".equals(languageInfo.code())) {
                LanguageDetectorService.LanguageDetectionResult langRes =
                        s.getTitle() != null && !s.getTitle().isBlank()
                                ? langDetector.detectLanguageForTrack(s.getTitle(), s.getArtist())
                                : langDetector.detectLanguage(s.getFileName());
                languageInfo = langRes.toLanguageInfo();
                s.setLanguageInfo(languageInfo);
            }
            String detectedLanguage = "Ambiguo".equals(languageInfo.name())
                    && languageInfo.alternatives() != null
                    && !languageInfo.alternatives().isBlank()
                            ? languageInfo.name() + " " + languageInfo.alternatives()
                            : languageInfo.name();
            candidate.setDetectedLanguage(detectedLanguage);
            candidate.setLanguageConfidence(languageInfo.confidence());

            candidates.add(candidate);
        }

        return candidates;
    }

    public OriginalScoreCalculator.QualityEvaluation evaluateQuality(SongFile song) {
        return selectionStrategy.evaluate(song);
    }
}
