package com.example.interfaz.service.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MusicFolderServiceTest {

    @Test
    void testGetCurrentMusicFolderNotNull() {
        MusicFolderService service = new MusicFolderService();
        assertNotNull(service.getCurrentMusicFolder());
        assertFalse(service.getCurrentMusicFolder().isEmpty());
    }

    @Test
    void testGetDisplayPath() {
        MusicFolderService service = new MusicFolderService();
        String displayPath = service.getDisplayPath();
        assertNotNull(displayPath);
        assertFalse(displayPath.isEmpty());
    }
}
