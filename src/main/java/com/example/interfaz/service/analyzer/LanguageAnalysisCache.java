package com.example.interfaz.service.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.analyzer.LanguageDetectionMethod;
import com.example.interfaz.model.analyzer.LanguageInfo;

public class LanguageAnalysisCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageAnalysisCache.class);
    private static final Path DEFAULT_CACHE_FILE = Path.of("config", "language-cache.properties");

    private final Path cacheFile;
    private final Properties entries = new Properties();

    public LanguageAnalysisCache() {
        this(DEFAULT_CACHE_FILE);
    }

    LanguageAnalysisCache(Path cacheFile) {
        this.cacheFile = cacheFile;
        load();
    }

    public synchronized LanguageInfo get(
            Path path,
            long size,
            long lastModified,
            int detectorVersion
    ) {
        if (path == null) {
            return null;
        }
        String value = entries.getProperty(key(path));
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length != 9) {
            return null;
        }
        try {
            if (Long.parseLong(parts[0]) != size
                    || Long.parseLong(parts[1]) != lastModified
                    || Integer.parseInt(parts[2]) != detectorVersion) {
                return null;
            }
            return new LanguageInfo(
                    parts[3],
                    parts[4],
                    Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]),
                    LanguageDetectionMethod.valueOf(parts[7]),
                    parts[8],
                    System.currentTimeMillis()
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public synchronized void put(
            Path path,
            long size,
            long lastModified,
            int detectorVersion,
            LanguageInfo info
    ) {
        if (path == null || info == null) {
            return;
        }
        String value = String.join("|",
                String.valueOf(size),
                String.valueOf(lastModified),
                String.valueOf(detectorVersion),
                info.code(),
                info.name(),
                String.valueOf(info.confidence()),
                String.valueOf(info.margin()),
                info.method().name(),
                info.alternatives());
        entries.setProperty(key(path), value);
    }

    public synchronized void save() {
        try {
            Path parent = cacheFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(cacheFile)) {
                entries.store(output, "Language analysis cache");
            }
        } catch (IOException e) {
            LOGGER.debug("Could not save language cache: {}", e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(cacheFile)) {
            return;
        }
        try (InputStream input = Files.newInputStream(cacheFile)) {
            entries.load(input);
        } catch (IOException e) {
            LOGGER.debug("Could not load language cache: {}", e.getMessage());
        }
    }

    private String key(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
