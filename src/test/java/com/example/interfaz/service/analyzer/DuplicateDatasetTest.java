package com.example.interfaz.service.analyzer;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDatasetTest {

    private final DuplicateDetectionService service = new DuplicateDetectionService();

    @Test
    void testMusicDatasetBehavior() {
        SongFile s1 = new SongFile(Path.of("/music/Queen - Bohemian Rhapsody.mp3"),
                "Queen - Bohemian Rhapsody.mp3", "queen bohemian rhapsody", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354, 10000000, "320kbps", "MP3");

        SongFile s2 = new SongFile(Path.of("/music/Queen - Bohemian Rhapsody Copy.mp3"),
                "Queen - Bohemian Rhapsody Copy.mp3", "queen bohemian rhapsody copy", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354, 9800000, "320kbps", "MP3");

        SongFile s3 = new SongFile(Path.of("/music/Queen - Bohemian Rhapsody Live.mp3"),
                "Queen - Bohemian Rhapsody Live.mp3", "queen bohemian rhapsody live", "Bohemian Rhapsody Live", "Queen", "A Night at the Opera", 380, 11000000, "320kbps", "MP3");

        List<SongFile> dataset = List.of(s1, s2, s3);
        List<DuplicateGroup> groups = service.detectDuplicates(dataset);

        assertFalse(groups.isEmpty(), "Groups should be detected");

        boolean hasConfirmed = groups.stream().anyMatch(g -> g.getClassification() == GroupClassification.CONFIRMED_DUPLICATE);
        boolean hasVariant = groups.stream().anyMatch(g -> g.getClassification() == GroupClassification.VERSION_VARIANT);

        assertTrue(hasConfirmed || hasVariant, "Dataset should categorize confirmed duplicates or version variants");
    }
}
