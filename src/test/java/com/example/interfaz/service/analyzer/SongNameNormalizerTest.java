package com.example.interfaz.service.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SongNameNormalizerTest {

    private SongNameNormalizer normalizer;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        normalizer = new SongNameNormalizer();
    }

    @Test
    void shouldNormalizeQueenBohemianRhapsodyWithOfficialVideo() {
        String input = "Queen - Bohemian Rhapsody (Official Video).mp3";
        String expected = "queen bohemian rhapsody";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void shouldRemoveBracketsAndNoiseWords() {
        String input = "Artist - Song Title [Remastered] (Lyrics) {Audio Oficial} HD 4K feat. Someone.flac";
        String expected = "artist song title someone";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    void shouldHandleNullAndEmptyString() {
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize("   "));
    }

    @Test
    void shouldNormalizeSimpleNameWithoutExtension() {
        String input = "Bohemian Rhapsody";
        assertEquals("bohemian rhapsody", normalizer.normalize(input));
    }
}
