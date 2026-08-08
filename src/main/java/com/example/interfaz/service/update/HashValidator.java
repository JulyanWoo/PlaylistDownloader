package com.example.interfaz.service.update;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashValidator.class);
    private final HttpClient httpClient;

    public HashValidator() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public HashValidator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String computeSha256(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest()).toLowerCase();
        } catch (Exception e) {
            LOGGER.error("Error calculando SHA-256 para {}", file, e);
            return "";
        }
    }

    public String fetchExpectedSha256(String sha256SumsUrl, String targetFileName) {
        if (sha256SumsUrl == null || sha256SumsUrl.isBlank()) {
            return "";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sha256SumsUrl))
                    .header("User-Agent", "PlaylistDownloader-App")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseHashFromSumsFile(response.body(), targetFileName);
            }
        } catch (java.io.IOException | InterruptedException e) {
            LOGGER.warn("No se pudo descargar SHA256SUMS desde GitHub: {}", e.getMessage());
        }
        return "";
    }

    public String parseHashFromSumsFile(String sumsContent, String targetFileName) {
        if (sumsContent == null || sumsContent.isBlank() || targetFileName == null || targetFileName.isBlank()) {
            return "";
        }

        Pattern pattern = Pattern.compile("([a-fA-F0-9]{64})\\s+[*\\s]?.*?" + Pattern.quote(targetFileName));
        Matcher matcher = pattern.matcher(sumsContent);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return "";
    }

    public boolean verifyFileHash(Path file, String sha256SumsUrl) {
        String expectedHash = fetchExpectedSha256(sha256SumsUrl, file.getFileName().toString());
        if (expectedHash.isBlank()) {
            LOGGER.warn("No se pudo obtener el hash SHA-256 esperado; omitiendo verificación estricta de checksum.");
            return true;
        }

        String computedHash = computeSha256(file);
        boolean matches = computedHash.equalsIgnoreCase(expectedHash);
        if (matches) {
            LOGGER.info("Hash SHA-256 verificado correctamente para {}: {}", file.getFileName(), computedHash);
        } else {
            LOGGER.error("Mismatch de SHA-256 en {}. Calculado: {}, Esperado: {}", file.getFileName(), computedHash, expectedHash);
        }
        return matches;
    }
}
