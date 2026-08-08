package com.example.interfaz.service.analyzer;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.interfaz.event.EventBus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LibraryAnalyzerFacadeTest {

    private LibraryAnalyzerFacade facade;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        EventBus eventBus = new EventBus();
        SongNameNormalizer normalizer = new SongNameNormalizer();
        SongMetadataReader metadataReader = new SongMetadataReader(normalizer);
        DuplicateDetectionService duplicateDetectionService = new DuplicateDetectionService();
        LibraryAnalyzerService libraryAnalyzerService = new LibraryAnalyzerService(
                null, metadataReader, duplicateDetectionService, eventBus
        );

        DuplicateSelectionService duplicateSelectionService = new DuplicateSelectionService();
        DuplicateManagementService duplicateManagementService = new DuplicateManagementService(libraryAnalyzerService);
        SongLanguageBrowserService songLanguageBrowserService = new SongLanguageBrowserService(null);

        facade = new LibraryAnalyzerFacade(
                libraryAnalyzerService,
                duplicateSelectionService,
                duplicateManagementService,
                songLanguageBrowserService,
                eventBus
        );
    }

    @Test
    void testCalculateSelectionSummaryOnEmptyGroups() {
        var summary = facade.calculateSelectionSummary(List.of());
        assertNotNull(summary);
        assertEquals(0, summary.selectedCount());
        assertEquals(0L, summary.bytesToFree());
    }

    @Test
    void testGetSelectedCandidatesOnEmptyGroups() {
        var selected = facade.getSelectedCandidates(List.of());
        assertNotNull(selected);
        assertEquals(0, selected.size());
    }

    @Test
    void testSubscribeAndClose() {
        facade.subscribeToEvents(
                started -> {},
                progress -> {},
                finished -> {},
                cancelled -> {}
        );

        facade.close();
    }
}
