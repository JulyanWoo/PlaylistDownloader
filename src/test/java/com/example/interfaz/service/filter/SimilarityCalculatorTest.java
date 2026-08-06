package com.example.interfaz.service.filter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityCalculatorTest {

    @Test
    void testExactMatchReturnsOne() {
        double similarity = SimilarityCalculator.calculateCombinedSimilarity("queen bohemian rhapsody", "queen bohemian rhapsody");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testSimilarTitlesHighSimilarity() {
        boolean similar = SimilarityCalculator.areSimilar("Queen - Bohemian Rhapsody (Official Video)", "Queen - Bohemian Rhapsody", 0.70);
        assertTrue(similar);
    }

    @Test
    void testDifferentTitlesLowSimilarity() {
        boolean similar = SimilarityCalculator.areSimilar("Queen - Bohemian Rhapsody", "Coldplay - Yellow", 0.70);
        assertFalse(similar);
    }

    @Test
    void testLevenshteinDistanceCalculation() {
        int distance = SimilarityCalculator.levenshteinDistance("kitten", "sitting");
        assertEquals(3, distance);
    }
}
