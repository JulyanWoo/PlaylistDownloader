package com.example.interfaz.service.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.interfaz.model.analyzer.ParsedTrackName;

class TitleParserServiceTest {

    private final TitleParserService parser = new TitleParserService();

    @Test
    void parsesBothCommonArtistTitleOrders() {
        assertTrack(
                "01. Muy Bandida - ALZATE ｜ Video Oficial.mp3",
                "ALZATE",
                "Muy Bandida"
        );
        assertTrack(
                "De mí Para mí (En Vivo) - Luis Alfonso x Jhon Alex Castaño ｜ Video Oficial.mp3",
                "Luis Alfonso x Jhon Alex Castaño",
                "De mí Para mí (En Vivo)"
        );
        assertTrack(
                "ALZATE -  YO YA NO VUELVO CONTIGO ｜ VIDEO OFICIAL.mp3",
                "ALZATE",
                "YO YA NO VUELVO CONTIGO"
        );
        assertTrack(
                "ALZATE & EL CHARRITO NEGRO - MI DESPEDIDA 🥃 ｜ VIDEO OFICIAL.mp3",
                "ALZATE & EL CHARRITO NEGRO",
                "MI DESPEDIDA 🥃"
        );
        assertTrack(
                "EL DESQUITE - ALZATE - (VIDEO OFICIAL).mp3",
                "ALZATE",
                "EL DESQUITE"
        );
    }

    @Test
    void preservesTheCompleteNameWhenTheOrderIsGenuinelyAmbiguous() {
        ParsedTrackName parsed = parser.parse("Neon Echo - Silver Horizon.mp3");

        assertEquals("", parsed.artist());
        assertEquals("Neon Echo - Silver Horizon", parsed.title());
    }

    private void assertTrack(String fileName, String expectedArtist, String expectedTitle) {
        ParsedTrackName parsed = parser.parse(fileName);

        assertEquals(expectedArtist, parsed.artist(), "Artista para " + fileName);
        assertEquals(expectedTitle, parsed.title(), "Título para " + fileName);
    }
}
