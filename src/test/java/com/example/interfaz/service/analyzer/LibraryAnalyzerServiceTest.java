package com.example.interfaz.service.analyzer;

import com.example.interfaz.event.EventBus;
import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.config.MusicFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class LibraryAnalyzerServiceTest {

    private LibraryAnalyzerService service;

    @BeforeEach
    void setUp() {
        MusicFolderService musicFolderService = new MusicFolderService();
        SongMetadataReader metadataReader = new SongMetadataReader();
        DuplicateDetectionService duplicateDetectionService = new DuplicateDetectionService();
        EventBus eventBus = new EventBus();

        service = new LibraryAnalyzerService(musicFolderService, metadataReader, duplicateDetectionService, eventBus);
    }

    @Test
    void shouldFilterNonAudioAndTempFiles(@TempDir Path tempDir) throws IOException {
        Path validMp3 = Files.createFile(tempDir.resolve("song.mp3"));
        Path tempFile = Files.createFile(tempDir.resolve("download.part"));
        Path txtFile = Files.createFile(tempDir.resolve("info.txt"));

        assertTrue(service.isAudioFile(validMp3));
        assertFalse(service.isAudioFile(tempFile));
        assertFalse(service.isAudioFile(txtFile));
    }

    @Test
    void shouldMoveSelectedCandidateToQuarantineFolder(@TempDir Path musicDir) throws IOException {
        MusicFolderService mockFolderService = new MusicFolderService() {
            @Override
            public String getCurrentMusicFolder() {
                return musicDir.toString();
            }
        };

        service = new LibraryAnalyzerService(mockFolderService, new SongMetadataReader(), new DuplicateDetectionService(), new EventBus());

        Path songPath = Files.createFile(musicDir.resolve("duplicate.mp3"));
        SongFile songFile = new SongFile(songPath, "duplicate.mp3", "duplicate", "Title", "Artist", "Album", 180, 5000, "320 kbps", "MP3");

        DuplicateCandidate candidate = new DuplicateCandidate(songFile);
        candidate.setSelectedForDeletion(true);
        candidate.setOriginal(false);

        int movedCount = service.moveToQuarantine(Collections.singletonList(candidate));

        assertEquals(1, movedCount);
        assertFalse(Files.exists(songPath), "Original file should have been moved");
        assertTrue(Files.exists(musicDir.resolve(".duplicates").resolve("duplicate.mp3")), "File should exist in .duplicates directory");
    }
}
