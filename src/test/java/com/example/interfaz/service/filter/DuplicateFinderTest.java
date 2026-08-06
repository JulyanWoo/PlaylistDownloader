package com.example.interfaz.service.filter;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class DuplicateFinderTest {

    @Test
    void testIsDuplicateExactMatch() {
        DuplicateFinder finder = new DuplicateFinder(0.70);
        Set<String> downloaded = new HashSet<>();
        downloaded.add("Queen - Bohemian Rhapsody");

        assertTrue(finder.isDuplicate("Queen - Bohemian Rhapsody", downloaded));
    }

    @Test
    void testIsDuplicateSimilarMatch() {
        DuplicateFinder finder = new DuplicateFinder(0.70);
        Set<String> downloaded = new HashSet<>();
        downloaded.add("Queen - Bohemian Rhapsody");

        assertTrue(finder.isDuplicate("Queen - Bohemian Rhapsody (Official Music Video)", downloaded));
    }

    @Test
    void testNotDuplicate() {
        DuplicateFinder finder = new DuplicateFinder(0.70);
        Set<String> downloaded = new HashSet<>();
        downloaded.add("Queen - Bohemian Rhapsody");

        assertFalse(finder.isDuplicate("Imagine Dragons - Believer", downloaded));
    }
}
