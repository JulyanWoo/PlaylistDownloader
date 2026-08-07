package com.example.interfaz.service.update;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCacheTest {

    @Test
    void testSaveAndLoadCache(@TempDir Path tempDir) {
        Path cacheFile = tempDir.resolve("config").resolve("update.json");
        UpdateCache cache = new UpdateCache(cacheFile);

        assertFalse(cache.isCacheValidHours(24), "Caché inexistente no debe ser válido");

        UpdateInfo info = new UpdateInfo("2026.03.17", "2026.07.04", true, "https://github.com/download/yt-dlp.exe");
        cache.saveCache(info);

        assertTrue(cache.isCacheValidHours(24), "Caché recién guardado debe ser válido dentro de 24h");

        UpdateInfo loaded = cache.getCachedUpdateInfo("2026.03.17");
        assertNotNull(loaded);
        assertEquals("2026.07.04", loaded.getLatestVersion());
        assertTrue(loaded.isUpdateAvailable());
        assertEquals("https://github.com/download/yt-dlp.exe", loaded.getDownloadUrl());
    }
}
