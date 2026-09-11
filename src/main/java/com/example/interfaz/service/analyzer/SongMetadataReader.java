package com.example.interfaz.service.analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.analyzer.ParsedTrackName;
import com.example.interfaz.model.analyzer.SongFile;

public class SongMetadataReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SongMetadataReader.class);
    private final SongNameNormalizer normalizer;
    private final TitleParserService titleParser;
    private final Set<String> knownArtistKeys = new HashSet<>();

    public SongMetadataReader() {
        this.normalizer = new SongNameNormalizer();
        this.titleParser = new TitleParserService();
    }

    public SongMetadataReader(SongNameNormalizer normalizer) {
        this.normalizer = normalizer != null ? normalizer : new SongNameNormalizer();
        this.titleParser = new TitleParserService();
    }

    public void primeKnownArtists(List<Path> audioFiles) {
        knownArtistKeys.clear();
        if (audioFiles == null || audioFiles.isEmpty()) {
            return;
        }

        Map<String, Integer> occurrences = new HashMap<>();
        for (Path audioFile : audioFiles) {
            if (audioFile == null || audioFile.getFileName() == null) {
                continue;
            }

            ParsedTrackName parsed = titleParser.parse(audioFile.getFileName().toString());
            if (parsed.artist().isBlank()) {
                continue;
            }

            String artistKey = titleParser.artistKey(parsed.artist());
            if (!artistKey.isBlank()) {
                occurrences.merge(artistKey, 1, Integer::sum);
            }
        }

        occurrences.forEach((artistKey, count) -> {
            if (count >= 2) {
                knownArtistKeys.add(artistKey);
            }
        });
    }

    public SongFile readMetadata(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }

        File file = path.toFile();
        String fileName = file.getName();
        long size = file.length();
        String format = getFileExtension(fileName).toUpperCase();

        String title = "";
        String artist = "";
        String album = "";
        long duration = 0;
        String bitrate = "";

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                duration = header.getTrackLength();
                bitrate = header.getBitRate() + " kbps";
                if (header.getFormat() != null && !header.getFormat().isEmpty()) {
                    format = header.getFormat();
                }
            }

            Tag tag = audioFile.getTag();
            if (tag != null) {
                title = tag.getFirst(FieldKey.TITLE);
                artist = tag.getFirst(FieldKey.ARTIST);
                album = tag.getFirst(FieldKey.ALBUM);
            }
        } catch (IOException | CannotReadException | InvalidAudioFrameException | ReadOnlyFileException | KeyNotFoundException | TagException e) {
            LOGGER.debug("Could not read tags for file: {} - {}", fileName, e.getMessage());
        }

        ParsedTrackName parsedName = titleParser.parse(fileName, knownArtistKeys);

        if (title == null || title.trim().isEmpty()) {
            title = parsedName.title();
        }
        if (artist == null || artist.trim().isEmpty()) {
            artist = parsedName.artist();
        }
        if (artist != null && !artist.trim().isEmpty()) {
            knownArtistKeys.add(titleParser.artistKey(artist));
        }

        String normalizedName = normalizer.normalize(title);

        return new SongFile(path, fileName, normalizedName, title, artist, album, duration, size, bitrate, format);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1)
                : "";
    }
}
