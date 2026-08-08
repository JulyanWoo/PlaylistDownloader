package com.example.interfaz.service.analyzer;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.LanguageDetectorMode;

import static org.junit.jupiter.api.Assertions.*;

class LanguageDetectorServiceTest {

    private final LanguageDetectorService service = new LanguageDetectorService();

    @Test
    void testSpanishDetection() {
        LanguageDetectorService.LanguageDetectionResult res = service.detectLanguage("Luis Miguel - Ahora Te Puedes Marchar");
        assertEquals("es", res.languageCode());
        assertEquals("Español", res.languageName());
        assertTrue(res.confidence() > 0.50);
    }

    @Test
    void testSpanishAccentedTitlesAndKeywords() {
        LanguageDetectorService.LanguageDetectionResult res1 = service.detectLanguage("Banda Los Recoditos - Mi Último Deseo");
        assertEquals("es", res1.languageCode());
        assertEquals("Español", res1.languageName());

        LanguageDetectorService.LanguageDetectionResult res2 = service.detectLanguage("Carlos Vives, Sebastián Yatra - Robarte un Beso");
        assertEquals("es", res2.languageCode());
        assertEquals("Español", res2.languageName());

        LanguageDetectorService.LanguageDetectionResult res3 = service.detectLanguage("KAROL G, Nicki Minaj - Tusa");
        assertEquals("es", res3.languageCode());
        assertEquals("Español", res3.languageName());
    }

    @Test
    void testEnglishDetection() {
        LanguageDetectorService.LanguageDetectionResult res = service.detectLanguage("Shakira - Hips Don't Lie");
        assertEquals("en", res.languageCode());
        assertEquals("Inglés", res.languageName());
    }

    @Test
    void testCjkDetection() {
        LanguageDetectorService.LanguageDetectionResult res = service.detectLanguage("RADWIMPS - 前前前世");
        assertEquals("ja_cjk", res.languageCode());
        assertEquals("Asiático (CJK)", res.languageName());
    }

    @Test
    void testAggressivenessModes() {
        String testTitle = "Ahora Resulta";

        LanguageDetectorService.LanguageDetectionResult conservative = service.detectLanguage(testTitle, LanguageDetectorMode.CONSERVATIVE);
        assertEquals("es", conservative.languageCode());

        LanguageDetectorService.LanguageDetectionResult aggressive = service.detectLanguage(testTitle, LanguageDetectorMode.AGGRESSIVE);
        assertEquals("es", aggressive.languageCode());
        assertTrue(aggressive.confidence() >= conservative.confidence());
    }

    @Test
    void testAmbiguousDetectionInConservativeMode() {
        LanguageDetectorService.LanguageDetectionResult res = service.detectLanguage("Xyz", LanguageDetectorMode.CONSERVATIVE);
        assertEquals("unknown", res.languageCode());
        assertEquals("Ambiguo", res.languageName());
    }

    @Test
    void testFilenameBasedDetection() {
        // Simulates filenames when MP3 tags are absent (empty title/artist)
        LanguageDetectorService.LanguageDetectionResult r1 = service.detectLanguage("KAROL G, Nicki Minaj - Tusa (Official Video).mp3");
        assertEquals("es", r1.languageCode(), "Tusa should be detected as Spanish from filename");

        LanguageDetectorService.LanguageDetectionResult r2 = service.detectLanguage("Banda Los Recoditos - Mi Ultimo Deseo (Version 30 Aniversario).mp3");
        assertEquals("es", r2.languageCode(), "Mi Ultimo Deseo should be detected as Spanish from filename");

        LanguageDetectorService.LanguageDetectionResult r3 = service.detectLanguage("Carlos Vives, Sebastian Yatra - Robarte un Beso (Official Video).mp3");
        assertEquals("es", r3.languageCode(), "Robarte un Beso should be detected as Spanish from filename");

        LanguageDetectorService.LanguageDetectionResult r4 = service.detectLanguage("Ahora Resulta - copia (2).mp3");
        assertEquals("es", r4.languageCode(), "Ahora Resulta should be detected as Spanish from filename");
    }
}
