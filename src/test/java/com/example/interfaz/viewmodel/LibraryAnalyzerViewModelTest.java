package com.example.interfaz.viewmodel;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.LibraryAnalysisResult;
import com.example.interfaz.model.analyzer.SongFile;

import static org.junit.jupiter.api.Assertions.*;

class LibraryAnalyzerViewModelTest {

    @Test
    void testViewModelStateLifecycle() {
        LibraryAnalyzerViewModel viewModel = new LibraryAnalyzerViewModel();

        assertFalse(viewModel.analyzingProperty().get());
        assertFalse(viewModel.statsVisibleProperty().get());

        viewModel.startAnalysis("/music");
        assertTrue(viewModel.analyzingProperty().get());
        assertTrue(viewModel.statusTextProperty().get().contains("/music"));

        viewModel.updateProgress(5, 10, 2, 3000);
        assertEquals(0.5, viewModel.progressProperty().get(), 0.001);
        assertTrue(viewModel.statusTextProperty().get().contains("5 / 10"));

        SongFile f1 = new SongFile(Path.of("/music/song.mp3"), "song.mp3", "song", "Song", "Artist", "Album", 180, 5000000, "320kbps", "MP3");
        SongFile f2 = new SongFile(Path.of("/music/song_copy.mp3"), "song_copy.mp3", "song copy", "Song Copy", "Artist", "Album", 180, 5000000, "320kbps", "MP3");
        DuplicateCandidate c1 = new DuplicateCandidate(f1, false, true);
        DuplicateCandidate c2 = new DuplicateCandidate(f2, false, false);
        DuplicateGroup group = new DuplicateGroup("Group 1", List.of(c1, c2), GroupClassification.CONFIRMED_DUPLICATE);

        LibraryAnalysisResult result = new LibraryAnalysisResult(10, 10, 2, 5000000, 180000, List.of(group));

        viewModel.finishAnalysis(result);
        assertFalse(viewModel.analyzingProperty().get());
        assertTrue(viewModel.statsVisibleProperty().get());
        assertEquals("10", viewModel.statTotalSongsTextProperty().get());
        assertEquals(1, viewModel.getCurrentGroups().size());
    }
}
