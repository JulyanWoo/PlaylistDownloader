package com.example.interfaz.service.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseChecker.class);
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest";
    private static final String DEFAULT_EXE_DOWNLOAD_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";

    private final HttpClient httpClient;

    public ReleaseChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public ReleaseChecker(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public UpdateInfo checkForUpdates(String currentVersion) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_RELEASES_API))
                    .header("User-Agent", "PlaylistDownloader-App")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                String latestVersion = extractTagName(body);
                String downloadUrl = extractAssetDownloadUrl(body);
                String sha256SumsUrl = extractSha256SumsUrl(body);

                if (downloadUrl.isEmpty()) {
                    downloadUrl = DEFAULT_EXE_DOWNLOAD_URL;
                }

                boolean available = isNewerVersionAvailable(currentVersion, latestVersion);
                LOGGER.info("Versión instalada: {}, Última versión en GitHub: {}, Actualización disponible: {}",
                        currentVersion, latestVersion, available);

                return new UpdateInfo(currentVersion, latestVersion, available, downloadUrl, sha256SumsUrl);
            } else {
                LOGGER.warn("Respuesta inesperada al consultar lanzamientos de GitHub: HTTP {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.warn("No se pudo comprobar actualización en GitHub: {}", e.getMessage());
        }

        return new UpdateInfo(currentVersion, currentVersion, false, DEFAULT_EXE_DOWNLOAD_URL);
    }

    public boolean isNewerVersionAvailable(String current, String latest) {
        if (current == null || current.isBlank() || current.equalsIgnoreCase("Desconocida")) {
            return true;
        }
        if (latest == null || latest.isBlank() || latest.equalsIgnoreCase("Desconocida")) {
            return false;
        }

        String cleanCurrent = current.replaceAll("[^0-9.]", "").trim();
        String cleanLatest = latest.replaceAll("[^0-9.]", "").trim();

        return cleanLatest.compareTo(cleanCurrent) > 0;
    }

    private String extractTagName(String json) {
        Pattern pattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String extractAssetDownloadUrl(String json) {
        Pattern pattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"(https://[^\"]+/yt-dlp\\.exe)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return DEFAULT_EXE_DOWNLOAD_URL;
    }

    private String extractSha256SumsUrl(String json) {
        Pattern pattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"(https://[^\"]+/SHA256SUMS)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
