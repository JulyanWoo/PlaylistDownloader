package com.example.interfaz.service.ui.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LibraryAnalyzerViewBinderTest {

    private LibraryAnalyzerViewBinder binder;
    private LibraryAnalyzerViewModel viewModel;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        binder = new LibraryAnalyzerViewBinder();
        viewModel = new LibraryAnalyzerViewModel();
    }

    @Test
    void testBindWithNullControlsSafely() {
        assertNotNull(binder);
        assertDoesNotThrow(() -> binder.bind(
                viewModel,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        ));
    }
}
