package com.example.interfaz.service.download;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YtDlpUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YtDlpUpdateService.class);
    private final BinaryResolver binaryResolver;

    public YtDlpUpdateService(BinaryResolver binaryResolver) {
        this.binaryResolver = binaryResolver;
    }

    public YtDlpUpdateService() {
        this(new BinaryResolver());
    }

    public String getCurrentVersion() {
        try {
            String ytDlpPath = binaryResolver.resolveYtDlpPath();
            ProcessBuilder pb = new ProcessBuilder(List.of(ytDlpPath, "--version"));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (process.waitFor() == 0 && line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error al obtener la versión de yt-dlp", e);
        }
        return "Desconocida";
    }

    public CompletableFuture<Boolean> updateYtDlpAsync(Consumer<String> logCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String ytDlpPath = binaryResolver.resolveYtDlpPath();
                if (logCallback != null) {
                    logCallback.accept("Iniciando actualización de yt-dlp (" + ytDlpPath + ")...");
                }

                ProcessBuilder pb = new ProcessBuilder(List.of(ytDlpPath, "-U"));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (logCallback != null) {
                            logCallback.accept(line);
                        }
                        LOGGER.info("[yt-dlp update] {}", line);
                    }
                }

                int exitCode = process.waitFor();
                boolean success = (exitCode == 0);
                if (logCallback != null) {
                    if (success) {
                        logCallback.accept("✓ yt-dlp se actualizó correctamente (Código: 0).");
                    } else {
                        logCallback.accept("❌ Error actualizando yt-dlp (Código de salida: " + exitCode + ").");
                    }
                }
                return success;
            } catch (Exception e) {
                LOGGER.error("Fallo al actualizar yt-dlp", e);
                if (logCallback != null) {
                    logCallback.accept("❌ Excepción al actualizar: " + e.getMessage());
                }
                return false;
            }
        });
    }
}
