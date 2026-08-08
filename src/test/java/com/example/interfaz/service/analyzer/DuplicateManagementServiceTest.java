package com.example.interfaz.service.analyzer;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.config.MusicFolderService;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateManagementServiceTest {

    @Test
    void testMoveSelectedToQuarantine() {
        SongFile f1 = new SongFile(Path.of("/music/song.mp3"), "song.mp3", "song", "Song", "Artist", "Album", 180, 5000000, "320kbps", "MP3");
        SongFile f2 = new SongFile(Path.of("/music/song_copy.mp3"), "song_copy.mp3", "song copy", "Song Copy", "Artist", "Album", 180, 5000000, "320kbps", "MP3");

        DuplicateCandidate c1 = new DuplicateCandidate(f1, false, true);
        DuplicateCandidate c2 = new DuplicateCandidate(f2, true, false);

        DuplicateGroup group = new DuplicateGroup("Song Group", List.of(c1, c2), GroupClassification.CONFIRMED_DUPLICATE);

        LibraryAnalyzerService analyzerService = new LibraryAnalyzerService(
                new MusicFolderService(),
                new SongMetadataReader(new SongNameNormalizer()),
                new DuplicateDetectionService(),
                null
        ) {
            @Override
            public int moveToQuarantine(List<DuplicateCandidate> candidates) {
                return candidates.size();
            }
        };

        DuplicateManagementService managementService = new DuplicateManagementService(analyzerService);

        var result = managementService.moveSelectedToQuarantine(List.of(c2), List.of(group));
        assertEquals(1, result.countMoved());
        assertTrue(result.remainingGroups().isEmpty(), "Group should be removed since less than 2 candidates remain");
    }
}
