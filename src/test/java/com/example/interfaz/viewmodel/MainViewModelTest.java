package com.example.interfaz.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainViewModelTest {

    private MainViewModel viewModel;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        viewModel = new MainViewModel();
    }

    @Test
    void testInitialPropertiesState() {
        assertNotNull(viewModel.musicFolderDisplayPathProperty());
        assertNotNull(viewModel.ytDlpVersionTextProperty());
        assertNotNull(viewModel.ytDlpStatusTextProperty());
        assertNotNull(viewModel.updateButtonTextProperty());
        assertNotNull(viewModel.updateButtonDisabledProperty());

        assertEquals("", viewModel.musicFolderDisplayPathProperty().get());
        assertEquals("Ver: --", viewModel.ytDlpVersionTextProperty().get());
        assertEquals("Estado: --", viewModel.ytDlpStatusTextProperty().get());
        assertEquals("Buscar actualización", viewModel.updateButtonTextProperty().get());
        assertFalse(viewModel.updateButtonDisabledProperty().get());
    }
}
