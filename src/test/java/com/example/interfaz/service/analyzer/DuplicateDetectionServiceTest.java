package com.example.interfaz.service.analyzer;

import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDetectionServiceTest {

    private DuplicateDetectionService service;

    @BeforeEach
    void setUp() {
        service = new DuplicateDetectionService();
    }

    @Test
    void shouldDetectDuplicatesWithinDurationTolerance(@TempDir Path tempDir) throws IOException {
        Path f1 = Files.createFile(tempDir.resolve("Queen - Bohemian Rhapsody (Official Video).mp3"));
        Path f2 = Files.createFile(tempDir.resolve("Queen - Bohemian Rhapsody.mp3"));

        Files.writeString(f1, "dummy-audio-content-same");
        Files.writeString(f2, "dummy-audio-content-same");

        SongFile song1 = new SongFile(f1, f1.getFileName().toString(), "queen bohemian rhapsody", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354, 1000, "320 kbps", "MP3");
        SongFile song2 = new SongFile(f2, f2.getFileName().toString(), "queen bohemian rhapsody", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 355, 950, "320 kbps", "MP3");

        List<DuplicateGroup> groups = service.detectDuplicates(Arrays.asList(song1, song2));

        assertEquals(1, groups.size());
        DuplicateGroup group = groups.get(0);
        assertEquals(GroupClassification.CONFIRMED_DUPLICATE, group.getClassification());
        assertEquals(2, group.getCandidates().size());
    }

    @Test
    void shouldNotGroupSongsWithDifferentDuration(@TempDir Path tempDir) throws IOException {
        Path f1 = Files.createFile(tempDir.resolve("Song A (Short).mp3"));
        Path f2 = Files.createFile(tempDir.resolve("Song A (Long).mp3"));

        SongFile song1 = new SongFile(f1, f1.getFileName().toString(), "song a", "Song A", "Artist", "Album", 120, 1000, "320 kbps", "MP3");
        SongFile song2 = new SongFile(f2, f2.getFileName().toString(), "song a", "Song A", "Artist", "Album", 300, 2000, "320 kbps", "MP3");

        List<DuplicateGroup> groups = service.detectDuplicates(Arrays.asList(song1, song2));

        assertTrue(groups.isEmpty(), "Songs with large duration differences (>2s) should not be grouped");
    }
}
