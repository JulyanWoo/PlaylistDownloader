package com.example.interfaz.service.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.interfaz.model.analyzer.LanguageDetectionMethod;
import com.example.interfaz.model.analyzer.LanguageInfo;

class LanguageAnalysisCacheTest {

    @Test
    void persistsAndInvalidatesLanguageResults(@TempDir Path tempDir) throws Exception {
        Path song = Files.writeString(tempDir.resolve("song.mp3"), "audio");
        Path cacheFile = tempDir.resolve("language-cache.properties");
        long size = Files.size(song);
        long modified = Files.getLastModifiedTime(song).toMillis();
        LanguageInfo info = new LanguageInfo(
                "es", "Español", 0.82, 0.82,
                LanguageDetectionMethod.TEXT, "", System.currentTimeMillis());

        LanguageAnalysisCache cache = new LanguageAnalysisCache(cacheFile);
        cache.put(song, size, modified, LanguageDetectorService.DETECTOR_VERSION, info);
        cache.save();

        LanguageAnalysisCache reloaded = new LanguageAnalysisCache(cacheFile);
        LanguageInfo cached = reloaded.get(
                song, size, modified, LanguageDetectorService.DETECTOR_VERSION);

        assertEquals("es", cached.code());
        assertEquals(0.82, cached.margin());
        assertEquals(LanguageDetectionMethod.TEXT, cached.method());
        assertNull(reloaded.get(song, size + 1, modified, LanguageDetectorService.DETECTOR_VERSION));
        assertNull(reloaded.get(song, size, modified, LanguageDetectorService.DETECTOR_VERSION + 1));
    }
}
