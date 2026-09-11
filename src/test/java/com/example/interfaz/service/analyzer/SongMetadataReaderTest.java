package com.example.interfaz.service.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.interfaz.model.analyzer.SongFile;

class SongMetadataReaderTest {

    @Test
    void usesTheFilenameParserWhenAnAudioFileHasNoTags(@TempDir Path tempDir) throws IOException {
        Path audio = Files.createFile(tempDir.resolve("Mala Mujer - ALZATE ｜ Audio Oficial.mp3"));

        SongFile song = new SongMetadataReader().readMetadata(audio);

        assertEquals("Mala Mujer", song.getTitle());
        assertEquals("ALZATE", song.getArtist());
        assertEquals("mala mujer", song.getNormalizedName());
    }

    @Test
    void resolvesAmbiguousOrderUsingArtistsRepeatedInTheLibrary(@TempDir Path tempDir) throws IOException {
        Path firstKnownArtist = Files.createFile(tempDir.resolve("Mala Mujer - ALZATE ｜ Audio Oficial.mp3"));
        Path secondKnownArtist = Files.createFile(tempDir.resolve("Muy Bandida - ALZATE ｜ Video Oficial.mp3"));
        Path ambiguous = Files.createFile(tempDir.resolve("ALZATE - MIENTEME.mp3"));

        SongMetadataReader reader = new SongMetadataReader();
        reader.primeKnownArtists(List.of(firstKnownArtist, secondKnownArtist, ambiguous));
        SongFile song = reader.readMetadata(ambiguous);

        assertEquals("MIENTEME", song.getTitle());
        assertEquals("ALZATE", song.getArtist());
    }
}
