package com.example.interfaz.service.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.analyzer.SongFile;

public class ExactHashDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExactHashDetector.class);
    private final LibraryCacheManager cacheManager;

    public ExactHashDetector(LibraryCacheManager cacheManager) {
        this.cacheManager = cacheManager != null ? cacheManager : new LibraryCacheManager();
    }

    public ExactHashDetector() {
        this(new LibraryCacheManager());
    }

    public boolean isExactMatch(SongFile a, SongFile b) {
        if (a == null || b == null) return false;
        if (a.getSize() <= 0 || a.getSize() != b.getSize()) return false;

        if (a.getHash() == null) {
            a.setHash(computeSha256(a.getPath()));
        }
        if (b.getHash() == null) {
            b.setHash(computeSha256(b.getPath()));
        }

        String hashA = a.getHash();
        String hashB = b.getHash();

        return hashA != null && !hashA.isEmpty() && hashA.equalsIgnoreCase(hashB);
    }

    public String computeSha256(Path path) {
        if (path == null || !Files.exists(path)) return "";

        long size = 0;
        long lastModified = 0;
        try {
            size = Files.size(path);
            lastModified = Files.getLastModifiedTime(path).toMillis();
            String cached = cacheManager.getCachedHash(path, size, lastModified);
            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        } catch (IOException ignored) {
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String calculated = hexString.toString();
            if (!calculated.isBlank() && size > 0) {
                cacheManager.putCachedHash(path, size, lastModified, calculated);
            }
            return calculated;
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("Failed to compute SHA-256 for path: {}", path, e);
            return "";
        }
    }
}
