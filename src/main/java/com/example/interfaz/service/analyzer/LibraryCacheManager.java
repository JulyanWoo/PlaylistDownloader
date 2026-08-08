package com.example.interfaz.service.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryCacheManager.class);
    private static final Path CACHE_FILE = Path.of("config", "library-cache.json");

    public record CacheEntry(String path, long size, long lastModified, String sha256) {}

    private final Map<String, CacheEntry> cache = new HashMap<>();

    public LibraryCacheManager() {
        loadCache();
    }

    public String getCachedHash(Path path, long size, long lastModified) {
        if (path == null) return null;
        String key = path.toAbsolutePath().toString();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.size() == size && entry.lastModified() == lastModified) {
            return entry.sha256();
        }
        return null;
    }

    public void putCachedHash(Path path, long size, long lastModified, String sha256) {
        if (path == null || sha256 == null || sha256.isBlank()) return;
        String key = path.toAbsolutePath().toString();
        cache.put(key, new CacheEntry(key, size, lastModified, sha256));
        saveCache();
    }

    public final void loadCache() {
        if (!Files.exists(CACHE_FILE)) return;
        try {
            List<String> lines = Files.readAllLines(CACHE_FILE);
            for (String line : lines) {
                if (line.contains("\"path\":")) {
                    int p1 = line.indexOf("\"path\":\"") + 8;
                    int p2 = line.indexOf("\", \"size\":");
                    int s1 = line.indexOf("\"size\":") + 7;
                    int s2 = line.indexOf(", \"lastModified\":");
                    int m1 = line.indexOf("\"lastModified\":") + 15;
                    int m2 = line.indexOf(", \"sha256\":");
                    int h1 = line.indexOf("\"sha256\":\"") + 10;
                    int h2 = line.lastIndexOf("\"}");

                    if (p1 > 7 && p2 > p1 && s1 > 6 && s2 > s1 && m1 > 14 && m2 > m1 && h1 > 9 && h2 > h1) {
                        String p = line.substring(p1, p2).replace("\\\\", "\\");
                        long s = Long.parseLong(line.substring(s1, s2).trim());
                        long m = Long.parseLong(line.substring(m1, m2).trim());
                        String h = line.substring(h1, h2);
                        cache.put(p, new CacheEntry(p, s, m, h));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.debug("Could not load library-cache.json: {}", e.getMessage());
        }
    }

    public final void saveCache() {
        try {
            Path parent = CACHE_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            List<String> lines = new ArrayList<>();
            lines.add("[");
            for (CacheEntry entry : cache.values()) {
                lines.add(String.format("  {\"path\":\"%s\", \"size\":%d, \"lastModified\":%d, \"sha256\":\"%s\"},",
                        entry.path().replace("\\", "\\\\"),
                        entry.size(),
                        entry.lastModified(),
                        entry.sha256()));
            }
            lines.add("]");
            Files.write(CACHE_FILE, lines);
        } catch (IOException e) {
            LOGGER.debug("Could not save library-cache.json: {}", e.getMessage());
        }
    }
}
