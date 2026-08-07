package com.example.interfaz.service.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryUpdater.class);
    private final HttpClient httpClient;

    public BinaryUpdater() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public BinaryUpdater(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean updateBinary(String targetPath, String downloadUrl, Consumer<String> logCallback) {
        if (targetPath == null || targetPath.isBlank()) {
            notify(logCallback, "Ruta de ejecutable no válida.");
            return false;
        }

        Path targetFile = Path.of(targetPath);
        Path parentDir = targetFile.getParent();
        if (parentDir == null || !Files.exists(parentDir)) {
            notify(logCallback, "Directorio del ejecutable no encontrado: " + parentPath(targetPath));
            return false;
        }

        Path newFile = parentDir.resolve("yt-dlp.new.exe");
        Path backupFile = parentDir.resolve("yt-dlp.backup.exe");

        try {
            notify(logCallback, "Descargando nuevo binario desde GitHub...");
            boolean downloaded = downloadFile(downloadUrl, newFile);
            if (!downloaded || !Files.exists(newFile) || Files.size(newFile) == 0) {
                notify(logCallback, "❌ Falló la descarga del nuevo binario.");
                cleanUpQuietly(newFile);
                return false;
            }

            notify(logCallback, "Validando ejecutable descargado...");
            boolean valid = validateBinary(newFile);
            if (!valid) {
                notify(logCallback, "❌ El ejecutable descargado no superó la prueba de validación (--version).");
                cleanUpQuietly(newFile);
                return false;
            }

            notify(logCallback, "Reemplazando binario de forma segura...");
            if (Files.exists(targetFile)) {
                Files.move(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(newFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                cleanUpQuietly(backupFile);
                notify(logCallback, "✓ Binario actualizado y reemplazado con éxito.");
                return true;
            } catch (Exception e) {
                LOGGER.error("Error reemplazando el binario, restaurando copia de seguridad...", e);
                notify(logCallback, "⚠️ Falló el reemplazo final, restaurando copia de seguridad...");
                if (Files.exists(backupFile)) {
                    Files.move(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                cleanUpQuietly(newFile);
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("Error durante la actualización del binario", e);
            notify(logCallback, "❌ Error de actualización: " + e.getMessage());
            cleanUpQuietly(newFile);
            return false;
        }
    }

    public boolean validateBinary(Path binaryPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(List.of(binaryPath.toString(), "--version"));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();
                return exitCode == 0 && line != null && !line.isBlank();
            }
        } catch (Exception e) {
            LOGGER.warn("Error validando el binario {}: {}", binaryPath, e.getMessage());
            return false;
        }
    }

    private boolean downloadFile(String downloadUrl, Path destination) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "PlaylistDownloader-App")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                try (InputStream is = response.body()) {
                    Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            } else {
                LOGGER.warn("Error al descargar archivo desde GitHub. HTTP status: {}", response.statusCode());
            }
        } catch (Exception e) {
            LOGGER.error("Excepción durante descarga de binario", e);
        }
        return false;
    }

    private void notify(Consumer<String> consumer, String msg) {
        if (consumer != null) {
            consumer.accept(msg);
        }
        LOGGER.info("[BinaryUpdater] {}", msg);
    }

    private void cleanUpQuietly(Path file) {
        try {
            if (file != null) {
                Files.deleteIfExists(file);
            }
        } catch (Exception ignored) {
        }
    }

    private String parentPath(String path) {
        File f = new File(path);
        return f.getParent() != null ? f.getParent() : "";
    }
}
