package com.example.interfaz.service.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.LanguageDetectionMethod;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;

class LanguageDetectorServiceTest {

    private final LanguageDetectorService service = new LanguageDetectorService();

    @Test
    void detectsSupportedLatinLanguages() {
        assertEquals("es", service.detectLanguage("Ahora te puedes marchar").languageCode());
        assertEquals("en", service.detectLanguage("Dancing in the dark tonight").languageCode());
        assertEquals("pt", service.detectLanguage("Você não sabe do meu coração").languageCode());
        assertEquals("fr", service.detectLanguage("Je veux danser avec toi").languageCode());
    }

    @Test
    void keepsReportedSpanishTitlesOutOfPortuguese() {
        List<String> titles = List.of(
                "El Desmadre",
                "El Embustero",
                "El Mejor",
                "El Mozo",
                "Amor Verdadero",
                "Borracho de Celos",
                "Así Soy Yo",
                "Besitos Por Botellas"
        );

        long portugueseResults = titles.stream()
                .map(service::detectLanguage)
                .filter(result -> "pt".equals(result.languageCode()))
                .count();

        assertEquals(0, portugueseResults);
        assertFalse("pt".equals(service.detectLanguage("Borracho de Celos").languageCode()));
        assertEquals("es", service.detectLanguage(
                "Borracho de Celos", LanguageDetectorMode.FAST).languageCode());
        assertEquals("es", service.detectLanguage("Así Soy Yo").languageCode());
    }

    @Test
    void leavesInsufficientTitlesAmbiguous() {
        LanguageDetectorService.LanguageDetectionResult result = service.detectLanguage("Amor");

        assertEquals("ambiguous", result.languageCode());
        assertEquals("Ambiguo", result.languageName());
        assertFalse(result.alternatives().isBlank());
        assertEquals(LanguageDetectionMethod.TEXT, result.method());
    }

    @Test
    void separatesJapaneseKoreanAndChineseScripts() {
        assertEquals("ja", service.detectLanguage("君の名は").languageCode());
        assertEquals("ko", service.detectLanguage("사랑해").languageCode());
        assertEquals("zh", service.detectLanguage("月亮代表我的心").languageCode());
    }

    @Test
    void usesDeterministicCharactersBeforeTheTextModel() {
        LanguageDetectorService.LanguageDetectionResult spanish = service.detectLanguage("¿Dónde estás, niña?");
        LanguageDetectorService.LanguageDetectionResult portuguese = service.detectLanguage("Canção do coração e não");
        LanguageDetectorService.LanguageDetectionResult french = service.detectLanguage("Cœur de la nuit");

        assertEquals("es", spanish.languageCode());
        assertEquals("pt", portuguese.languageCode());
        assertEquals("fr", french.languageCode());
        assertEquals(LanguageDetectionMethod.UNICODE, spanish.method());
        assertEquals(LanguageDetectionMethod.UNICODE, portuguese.method());
        assertEquals(LanguageDetectionMethod.UNICODE, french.method());
    }

    @Test
    void cleansFileAndMusicMetadata() {
        assertEquals("Gasolina", service.cleanTitle(
                "Daddy Yankee - Gasolina (Official Video) [HD].mp3"));
        assertEquals("Amor", service.cleanTitle("Amor (Remix).flac"));
        assertEquals("Stay", service.cleanTitle("Stay feat. Artist (Official Audio).wav"));
    }

    @Test
    void doesNotUseArtistAsLinguisticInput() {
        LanguageDetectorService.LanguageDetectionResult titleOnly =
                service.detectLanguage("Mi último deseo");
        LanguageDetectorService.LanguageDetectionResult withArtist =
                service.detectLanguageForTrack("Mi último deseo", "The English Band");

        assertEquals(titleOnly.languageCode(), withArtist.languageCode());
        assertEquals(titleOnly.margin(), withArtist.margin());
    }

    @Test
    void exposesDecisionMarginAndMethod() {
        LanguageDetectorService.LanguageDetectionResult result =
                service.detectLanguage("Dancing in the dark tonight");

        assertEquals("en", result.languageCode());
        assertTrue(result.margin() > 0.0);
        assertEquals(result.margin(), result.confidence());
        assertEquals(LanguageDetectionMethod.TEXT, result.method());
    }

    @Test
    void preciseModeIsMoreConservativeThanFastMode() {
        String title = "Secreto de Amor";
        LanguageDetectorService.LanguageDetectionResult fast =
                service.detectLanguage(title, LanguageDetectorMode.FAST);
        LanguageDetectorService.LanguageDetectionResult precise =
                service.detectLanguage(title, LanguageDetectorMode.PRECISE);

        if ("ambiguous".equals(fast.languageCode())) {
            assertEquals("ambiguous", precise.languageCode());
        }
        assertTrue(precise.margin() >= 0.0);
    }

    @Test
    void detectsExplicitMixedMetadata() {
        LanguageDetectorService.LanguageDetectionResult result =
                service.detectLanguage("My Love Spanish-English Version");

        assertEquals("mixed", result.languageCode());
        assertEquals(LanguageDetectionMethod.METADATA, result.method());
    }

    @Test
    void meetsMinimumAccuracyOnCuratedTitles() {
        List<String> spanish = List.of(
                "Ahora Te Puedes Marchar",
                "Borracho de Celos",
                "Amigos con Derechos",
                "El Mejor de Mis Recuerdos",
                "Así Soy Yo",
                "Besitos Por Botellas",
                "Mi Último Deseo",
                "Robarte un Beso",
                "No Sufriré Por Nadie",
                "Amor Verdadero Para Siempre"
        );
        List<String> english = List.of(
                "Dancing in the Dark",
                "Thinking Out Loud",
                "I Will Always Love You",
                "Don't Stop Me Now",
                "Wake Me Up Before You Go",
                "Nothing Else Matters",
                "The Sound of Silence",
                "Everybody Wants to Rule the World",
                "You Are Not Alone",
                "Walking on Sunshine"
        );
        List<String> portuguese = List.of(
                "Você Não Sabe do Meu Coração",
                "Canção Para Minha Vida",
                "Tudo Que Você Quiser",
                "Meu Amor Não Vai Embora",
                "Saudade da Minha Terra",
                "Quando a Chuva Passar",
                "Eu Sei Que Vou Te Amar",
                "Não Quero Dinheiro",
                "A Vida Toda Com Você",
                "Nosso Sonho de Amor"
        );
        List<String> french = List.of(
                "Je Veux Danser Avec Toi",
                "La Vie en Rose",
                "Quand On N'a Que L'amour",
                "Je Ne Regrette Rien",
                "Sous le Ciel de Paris",
                "Pour Que Tu M'aimes Encore",
                "Le Temps des Fleurs",
                "Moi Je Joue",
                "La Mer et le Soleil",
                "Tous les Garçons et les Filles"
        );

        assertTrue(accuracy(spanish, "es") >= 0.80);
        assertTrue(accuracy(english, "en") >= 0.80);
        assertTrue(accuracy(portuguese, "pt") >= 0.80);
        assertTrue(accuracy(french, "fr") >= 0.80);
    }

    private double accuracy(List<String> titles, String expectedCode) {
        long correct = titles.stream()
                .map(title -> service.detectLanguage(title, LanguageDetectorMode.FAST))
                .filter(result -> expectedCode.equals(result.languageCode()))
                .count();
        return (double) correct / titles.size();
    }
}
