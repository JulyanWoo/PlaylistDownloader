package com.example.interfaz.service.ui.analyzer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AudioPreviewServiceTest {

    @Test
    void supportsNativePreviewFormatsCaseInsensitively() {
        assertTrue(AudioPreviewService.isSupported(Path.of("song.MP3")));
        assertTrue(AudioPreviewService.isSupported(Path.of("song.aac")));
        assertTrue(AudioPreviewService.isSupported(Path.of("song.m4a")));
        assertTrue(AudioPreviewService.isSupported(Path.of("song.wav")));

        assertFalse(AudioPreviewService.isSupported(Path.of("song.flac")));
        assertFalse(AudioPreviewService.isSupported(Path.of("song")));
        assertFalse(AudioPreviewService.isSupported(null));
    }

    @Test
    void requiresAnExistingSupportedFileForPreview() throws IOException {
        Path previewFile = Files.createTempFile("playlist-preview-", ".mp3");
        try {
            assertTrue(AudioPreviewService.isPreviewAvailable(previewFile));
            assertFalse(AudioPreviewService.isPreviewAvailable(previewFile.resolveSibling("missing.mp3")));
        } finally {
            Files.deleteIfExists(previewFile);
        }
    }
}
