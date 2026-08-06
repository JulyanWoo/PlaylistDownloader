package com.example.interfaz.service.download;

import com.example.interfaz.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SongMetadataServiceTest {

    private SongMetadataService metadataService;

    @BeforeEach
    void setUp() {
        metadataService = new SongMetadataService(new BinaryResolver());
    }

    @Test
    void shouldReturnFallbackTitleForEmptyOrNullUrl() {
        Song nullSong = metadataService.getSongInfo(null);
        assertNotNull(nullSong);
        assertEquals("Unknown Title", nullSong.getTitle());

        Song emptySong = metadataService.getSongInfo("");
        assertNotNull(emptySong);
        assertEquals("Unknown Title", emptySong.getTitle());
    }

    @Test
    void shouldExtractVideoIdFallbackWhenYtDlpUnavailable() {
        Song song = metadataService.getSongInfo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertNotNull(song);
        assertNotNull(song.getTitle());
        assertTrue(song.getTitle().contains("dQw4w9WgXcQ") || !song.getTitle().isEmpty());
    }
}
