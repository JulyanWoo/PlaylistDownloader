package com.example.interfaz.service.analyzer;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateSelectionServiceTest {

    private final DuplicateSelectionService selectionService = new DuplicateSelectionService();

    private DuplicateGroup createSampleGroup() {
        SongFile f1 = new SongFile(Path.of("/music/song.mp3"), "song.mp3", "song", "Song", "Artist", "Album", 180, 5000000, "320kbps", "MP3");
        SongFile f2 = new SongFile(Path.of("/music/song_copy.mp3"), "song_copy.mp3", "song copy", "Song Copy", "Artist", "Album", 180, 5000000, "320kbps", "MP3");

        DuplicateCandidate c1 = new DuplicateCandidate(f1, false, true);
        DuplicateCandidate c2 = new DuplicateCandidate(f2, false, false);

        return new DuplicateGroup("Song Group", List.of(c1, c2), GroupClassification.CONFIRMED_DUPLICATE);
    }

    @Test
    void testSelectDuplicates() {
        DuplicateGroup group = createSampleGroup();
        selectionService.selectDuplicates(List.of(group));

        assertFalse(group.getCandidates().get(0).isSelectedForDeletion(), "Original no debe estar seleccionado para eliminación");
        assertTrue(group.getCandidates().get(1).isSelectedForDeletion(), "Copia debe estar seleccionada para eliminación");
    }

    @Test
    void testKeepOriginals() {
        DuplicateGroup group = createSampleGroup();
        group.getCandidates().get(0).setSelectedForDeletion(true);

        selectionService.keepOriginals(List.of(group));
        assertFalse(group.getCandidates().get(0).isSelectedForDeletion());
        assertTrue(group.getCandidates().get(1).isSelectedForDeletion());
    }

    @Test
    void testUnselectAll() {
        DuplicateGroup group = createSampleGroup();
        selectionService.selectDuplicates(List.of(group));

        selectionService.unselectAll(List.of(group));
        assertFalse(group.getCandidates().get(0).isSelectedForDeletion());
        assertFalse(group.getCandidates().get(1).isSelectedForDeletion());
    }

    @Test
    void testCalculateSelectionSummary() {
        DuplicateGroup group = createSampleGroup();
        selectionService.selectDuplicates(List.of(group));

        var summary = selectionService.calculateSelectionSummary(List.of(group));
        assertEquals(1, summary.selectedCount());
        assertEquals(5000000, summary.bytesToFree());
        assertTrue(summary.getFormattedSummary().contains("1 duplicados seleccionados"));
    }
}
