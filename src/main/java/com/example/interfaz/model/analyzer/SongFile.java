package com.example.interfaz.model.analyzer;

import java.nio.file.Path;

public class SongFile {

    private final Path path;
    private final String fileName;
    private final String normalizedName;
    private final String title;
    private final String artist;
    private final String album;
    private final long duration; // In seconds
    private final long size;     // In bytes
    private final String bitrate;
    private final String format;
    private String hash;         // Computed lazily in level 4

    public SongFile(Path path, String fileName, String normalizedName, String title, String artist,
                    String album, long duration, long size, String bitrate, String format) {
        this.path = path;
        this.fileName = fileName;
        this.normalizedName = normalizedName;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.size = size;
        this.bitrate = bitrate;
        this.format = format;
    }

    public Path getPath() {
        return path;
    }

    public String getFileName() {
        return fileName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public long getSize() {
        return size;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getFormat() {
        return format;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
