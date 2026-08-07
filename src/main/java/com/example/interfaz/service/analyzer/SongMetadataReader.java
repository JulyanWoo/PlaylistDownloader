package com.example.interfaz.service.analyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

import com.example.interfaz.model.analyzer.SongFile;

public class SongMetadataReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SongMetadataReader.class);
    private final SongNameNormalizer normalizer;

    public SongMetadataReader() {
        this.normalizer = new SongNameNormalizer();
    }

    public SongMetadataReader(SongNameNormalizer normalizer) {
        this.normalizer = normalizer != null ? normalizer : new SongNameNormalizer();
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

        String rawNameForNormalization = (title != null && !title.trim().isEmpty())
                ? (artist != null && !artist.trim().isEmpty() ? artist + " " + title : title)
                : fileName;

        String normalizedName = normalizer.normalize(rawNameForNormalization);

        return new SongFile(path, fileName, normalizedName, title, artist, album, duration, size, bitrate, format);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1)
                : "";
    }
}
