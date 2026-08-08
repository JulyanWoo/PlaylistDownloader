package com.example.interfaz.service.analyzer;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateSimilarityServiceTest {

    private final DuplicateSimilarityService similarityService = new DuplicateSimilarityService();

    @Test
    void testFuzzyDuplicateDetection() {
        SongFile s1 = new SongFile(Path.of("/music/Ed Sheeran - Perfect (Official Music Video) 4K.mp3"),
                "Ed Sheeran - Perfect (Official Music Video) 4K.mp3", "ed sheeran perfect", "Perfect", "Ed Sheeran", "Divide", 263, 10000000, "320kbps", "MP3");
        SongFile s2 = new SongFile(Path.of("/music/Ed Sheeran - Perfect - Copia.mp3"),
                "Ed Sheeran - Perfect - Copia.mp3", "ed sheeran perfect", "Perfect", "Ed Sheeran", "Divide", 264, 9800000, "320kbps", "MP3");

        double score = similarityService.calculateSimilarityScore(s1, s2);
        assertTrue(score >= 0.90, "Fuzzy score for normalized Ed Sheeran Perfect should be >= 0.90, actual: " + score);

        GroupClassification classification = similarityService.classifySimilarity(score, false, false);
        assertEquals(GroupClassification.CONFIRMED_DUPLICATE, classification);
    }

    @Test
    void testRemixModifierPenalty() {
        SongFile original = new SongFile(Path.of("/music/Queen - Somebody To Love.mp3"),
                "Queen - Somebody To Love.mp3", "queen somebody to love", "Somebody To Love", "Queen", "Album", 296, 9000000, "320kbps", "MP3");
        SongFile remix = new SongFile(Path.of("/music/Queen - Somebody To Love Remix.mp3"),
                "Queen - Somebody To Love Remix.mp3", "queen somebody to love remix", "Somebody To Love Remix", "Queen", "Album", 296, 9000000, "320kbps", "MP3");

        double score = similarityService.calculateSimilarityScore(original, remix);
        assertTrue(score < 0.90, "Remix vs Original should have a modifier penalty reducing score below 0.90, actual: " + score);

        GroupClassification classification = similarityService.classifySimilarity(score, false, true);
        assertEquals(GroupClassification.VERSION_VARIANT, classification);
    }

    @Test
    void testDifferentSongsNotDuplicate() {
        SongFile s1 = new SongFile(Path.of("/music/Luis Miguel - Ahora Te Puedes Marchar.mp3"),
                "Luis Miguel - Ahora Te Puedes Marchar.mp3", "luis miguel ahora te puedes marchar", "Ahora Te Puedes Marchar", "Luis Miguel", "Album", 195, 8000000, "320kbps", "MP3");
        SongFile s2 = new SongFile(Path.of("/music/Queen - Bohemian Rhapsody.mp3"),
                "Queen - Bohemian Rhapsody.mp3", "queen bohemian rhapsody", "Bohemian Rhapsody", "Queen", "Album", 354, 12000000, "320kbps", "MP3");

        double score = similarityService.calculateSimilarityScore(s1, s2);
        assertTrue(score < 0.65, "Different songs should have score < 0.65, actual: " + score);
    }

    @Test
    void testSameSongDifferentLanguageVersion() {
        SongFile orig = new SongFile(Path.of("/music/Luis Fonsi - Despacito.mp3"),
                "Luis Fonsi - Despacito.mp3", "luis fonsi despacito", "Despacito", "Luis Fonsi", "Vida", 228, 9000000, "320kbps", "MP3");

        SongFile engVer = new SongFile(Path.of("/music/Luis Fonsi - Despacito English Version.mp3"),
                "Luis Fonsi - Despacito English Version.mp3", "luis fonsi despacito english version", "Despacito English Version", "Luis Fonsi", "Vida", 228, 9100000, "320kbps", "MP3");

        double score = similarityService.calculateSimilarityScore(orig, engVer);
        assertTrue(score >= 0.75, "Different language version should maintain score >= 0.75, actual: " + score);

        GroupClassification classification = similarityService.classifySimilarity(score, false, false);
        assertEquals(GroupClassification.POSSIBLE_DUPLICATE, classification);
    }
}
