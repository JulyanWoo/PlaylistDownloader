package com.example.interfaz.service.analyzer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.AnalyzerProfile;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateLanguageAndProfileTest {

    @Test
    void testLargeSimulatedLibraryPerformanceAndProfiles() {
        List<SongFile> library = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            String title = "Track Number " + i;
            SongFile song = new SongFile(
                    Path.of("/music/" + title + ".mp3"),
                    title + ".mp3",
                    title.toLowerCase(),
                    title,
                    "Artist " + (i % 50),
                    "Album " + (i % 20),
                    200 + (i % 10),
                    8000000 + (i * 100),
                    "320kbps",
                    "MP3"
            );
            library.add(song);
        }

        // Add specific duplicate copies and language variants
        SongFile dup1 = new SongFile(Path.of("/music/Track Number 50 - Copy.mp3"), "Track Number 50 - Copy.mp3", "track number 50 copy", "Track Number 50", "Artist 0", "Album 10", 200, 8000000, "320kbps", "MP3");
        SongFile engVer = new SongFile(Path.of("/music/Track Number 50 English Version.mp3"), "Track Number 50 English Version.mp3", "track number 50 english version", "Track Number 50 English Version", "Artist 0", "Album 10", 200, 8100000, "320kbps", "MP3");

        library.add(dup1);
        library.add(engVer);

        DuplicateSimilarityService similarityService = new DuplicateSimilarityService();
        DuplicateDetectionService detectionService = new DuplicateDetectionService(similarityService, new SongNormalizer(), new ExactHashDetector(), new com.example.interfaz.service.analyzer.strategy.QualityFirstStrategy());

        long start = System.currentTimeMillis();
        List<DuplicateGroup> groups = detectionService.detectDuplicates(library);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 2000, "1000 songs analysis should finish under 2000ms, actual: " + elapsed + "ms");
        assertFalse(groups.isEmpty(), "Duplicates should be detected in large simulated library");

        // Profile comparison
        GroupClassification conservative = similarityService.classifySimilarity(0.96, false, false, AnalyzerProfile.CONSERVATIVE);
        assertEquals(GroupClassification.POSSIBLE_DUPLICATE, conservative);

        GroupClassification aggressive = similarityService.classifySimilarity(0.92, false, false, AnalyzerProfile.AGGRESSIVE);
        assertEquals(GroupClassification.CONFIRMED_DUPLICATE, aggressive);
    }
}
