package com.example.interfaz.service.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.Song;

public class SongMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SongMetadataService.class);
    private final BinaryResolver binaryResolver;
    private final ConcurrentHashMap<String, Song> cache = new ConcurrentHashMap<>();

    public SongMetadataService(BinaryResolver binaryResolver) {
        this.binaryResolver = binaryResolver;
    }

    public SongMetadataService() {
        this(new BinaryResolver());
    }

    public Song getSongInfo(String url) {
        if (url == null || url.trim().isEmpty()) {
            Song s = new Song();
            s.setTitle("Unknown Title");
            s.setUrl(url);
            return s;
        }
        String trimmedUrl = url.trim();
        return cache.computeIfAbsent(trimmedUrl, key -> {
            String title = fetchTitleWithYtDlp(key);
            Song song = new Song();
            song.setUrl(key);
            song.setTitle(title != null && !title.isBlank() ? title : extractFallbackTitle(key));
            return song;
        });
    }

    private String fetchTitleWithYtDlp(String url) {
        try {
            String ytDlp = binaryResolver.resolveYtDlpPath();
            List<String> command = new ArrayList<>();
            command.add(ytDlp);
            command.add("--get-title");
            command.add("--no-warnings");
            command.add("--skip-download");
            command.add(url);

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String titleLine = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && titleLine != null && !titleLine.trim().isEmpty()) {
                    return titleLine.trim();
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.warn("Could not fetch title via yt-dlp for {}: {}", url, e.getMessage());
        }
        return null;
    }

    private String extractFallbackTitle(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String query = uri.getQuery();
            if (query != null && query.contains("v=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("v=")) {
                        return "Video (" + param.substring(2) + ")";
                    }
                }
            }
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                return path.substring(path.lastIndexOf('/') + 1);
            }
        } catch (Exception ignored) {}
        return url;
    }
}
