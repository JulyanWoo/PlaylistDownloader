package com.example.interfaz.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YouTubeDownloadServiceTest {

    @Test
    void testCanHandleValidYouTubeUrls() {
        try (YouTubeDownloadService service = new YouTubeDownloadService()) {
            assertTrue(service.canHandle("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
            assertTrue(service.canHandle("https://youtu.be/dQw4w9WgXcQ"));
            assertTrue(service.canHandle("http://music.youtube.com/playlist?list=123"));
        }
    }

    @Test
    void testCanHandleInvalidUrls() {
        try (YouTubeDownloadService service = new YouTubeDownloadService()) {
            assertFalse(service.canHandle("https://fakeyoutube.com/watch?v=123"));
            assertFalse(service.canHandle("texto-youtube.com"));
            assertFalse(service.canHandle(null));
            assertFalse(service.canHandle(""));
        }
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenClosed() {
        YouTubeDownloadService service = new YouTubeDownloadService();
        service.close();

        assertNotNull(assertThrows(IllegalStateException.class, () -> service.downloadSong("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
        assertNotNull(assertThrows(IllegalStateException.class, () -> service.downloadPlaylist("https://www.youtube.com/playlist?list=123", "", true)));
    }
}
