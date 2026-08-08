package com.example.interfaz.service.ui.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LanguageBrowserTableConfiguratorTest {

    private LanguageBrowserTableConfigurator configurator;
    private LibraryAnalyzerViewModel viewModel;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        configurator = new LanguageBrowserTableConfigurator();
        viewModel = new LibraryAnalyzerViewModel();
    }

    @Test
    void testInitialization() {
        assertNotNull(configurator);
        assertDoesNotThrow(() -> configurator.configure(
                null, null, null, null, null, null, null, null, null,
                null, null, null, viewModel
        ));
    }
}
