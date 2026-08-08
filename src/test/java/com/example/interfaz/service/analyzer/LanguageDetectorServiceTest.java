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
    void testDiacriticsPreFilter() {
        LanguageDetectorService.LanguageDetectionResult esRes = service.detectLanguage("¿Dónde estás, niña?");
        assertEquals("es", esRes.languageCode());

        LanguageDetectorService.LanguageDetectionResult frRes = service.detectLanguage("Garçon et française à la plage");
        assertEquals("fr", frRes.languageCode());

        LanguageDetectorService.LanguageDetectionResult ptRes = service.detectLanguage("Canção do coração e não da razão");
        assertEquals("pt", ptRes.languageCode());
    }

    @Test
    void testSeparateTitleArtistWeighting() {
        // "Yeison Jimenez" as artist should not override a clear Spanish title "El Último Adiós"
        LanguageDetectorService.LanguageDetectionResult res = service.detectLanguageForTrack("El Último Adiós", "Yeison Jimenez");
        assertEquals("es", res.languageCode());
        assertTrue(res.confidence() >= 0.50);
    }

    @Test
    void testBigramsAndMorphology() {
        LanguageDetectorService.LanguageDetectionResult esRes = service.detectLanguage("Llorar Quiero");
        assertEquals("es", esRes.languageCode());

        LanguageDetectorService.LanguageDetectionResult enRes = service.detectLanguage("Thinking Somewhere");
        assertEquals("en", enRes.languageCode());
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
