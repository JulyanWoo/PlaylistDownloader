package com.example.interfaz.service.ui.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.interfaz.event.EventBus;
import com.example.interfaz.service.analyzer.DuplicateDetectionService;
import com.example.interfaz.service.analyzer.DuplicateManagementService;
import com.example.interfaz.service.analyzer.DuplicateSelectionService;
import com.example.interfaz.service.analyzer.LibraryAnalyzerFacade;
import com.example.interfaz.service.analyzer.LibraryAnalyzerService;
import com.example.interfaz.service.analyzer.SongLanguageBrowserService;
import com.example.interfaz.service.analyzer.SongMetadataReader;
import com.example.interfaz.service.analyzer.SongNameNormalizer;
import com.example.interfaz.service.ui.DialogService;
import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LibraryAnalyzerPresenterTest {

    private LibraryAnalyzerPresenter presenter;

    private static class StubDialogService extends DialogService {
        @Override
        public void showInfo(String title, String message) {}
        @Override
        public void showError(String title, String message) {}
        @Override
        public void showWarning(String title, String message) {}
        @Override
        public void showConfirmation(String title, String headerText, String contentText, Runnable onConfirm) {}
    }

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

        LibraryAnalyzerFacade facade = new LibraryAnalyzerFacade(
                libraryAnalyzerService,
                duplicateSelectionService,
                duplicateManagementService,
                songLanguageBrowserService,
                eventBus
        );

        LibraryAnalyzerViewModel viewModel = new LibraryAnalyzerViewModel();
        LibraryAnalyzerViewBinder binder = new LibraryAnalyzerViewBinder();
        AnalyzerTableConfigurator tableConfigurator = new AnalyzerTableConfigurator();
        LanguageBrowserTableConfigurator languageConfigurator = new LanguageBrowserTableConfigurator();
        DialogService dialogService = new StubDialogService();

        presenter = new LibraryAnalyzerPresenter(
                facade,
                viewModel,
                binder,
                tableConfigurator,
                languageConfigurator,
                dialogService
        );
    }

    @Test
    void testPresenterInitializationAndActions() {
        assertNotNull(presenter);
        assertDoesNotThrow(() -> presenter.initialize(null));
        assertDoesNotThrow(() -> presenter.keepOriginals());
        assertDoesNotThrow(() -> presenter.selectDuplicates());
        assertDoesNotThrow(() -> presenter.unselectAll());
        assertDoesNotThrow(() -> presenter.selectAllLanguages());
        assertDoesNotThrow(() -> presenter.deselectAllLanguages());
        assertDoesNotThrow(() -> presenter.moveSelectedToTrash());
        assertDoesNotThrow(() -> presenter.quarantineSelectedLanguages());
        assertDoesNotThrow(() -> presenter.close());
    }
}
