package com.example.interfaz.model.analyzer;

public record ParsedTrackName(String raw, String artist, String title) {
    public ParsedTrackName {
        raw = raw == null ? "" : raw;
        artist = artist == null ? "" : artist;
        title = title == null ? "" : title;
    }
}
