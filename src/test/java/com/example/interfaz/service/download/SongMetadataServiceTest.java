package com.example.interfaz.service.download;

import com.example.interfaz.model.Song;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies SongMetadataService fallback behaviour when yt-dlp is unavailable.
 */
class SongMetadataServiceTest {

    private final SongMetadataService metadataService = new SongMetadataService();

    @Test
    void shouldReturnFallbackForNullUrl() {
        Song song = metadataService.getSongInfo(null);
        assertNotNull(song);
        assertEquals("Unknown Title", song.getTitle());
    }

    @Test
    void shouldReturnFallbackForEmptyUrl() {
        Song song = metadataService.getSongInfo("  ");
        assertNotNull(song);
        assertEquals("Unknown Title", song.getTitle());
    }

    @Test
    void shouldReturnNonNullTitleForValidUrl() {
        // yt-dlp may not be available in CI — we only assert a non-null, non-empty title
        Song song = metadataService.getSongInfo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertNotNull(song);
        assertNotNull(song.getTitle());
        assertFalse(song.getTitle().isBlank(), "Title should not be blank (fallback or real)");
    }

    @Test
    void shouldCacheResultsForSameUrl() {
        String url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        Song first  = metadataService.getSongInfo(url);
        Song second = metadataService.getSongInfo(url);
        assertSame(first, second, "Should return cached Song instance for same URL");
    }
}
