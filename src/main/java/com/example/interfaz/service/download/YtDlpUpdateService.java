package com.example.interfaz.service.download;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.service.update.BinaryUpdater;
import com.example.interfaz.service.update.ReleaseChecker;
import com.example.interfaz.service.update.UpdateCache;
import com.example.interfaz.service.update.UpdateHistory;
import com.example.interfaz.service.update.UpdateInfo;

public class YtDlpUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YtDlpUpdateService.class);
    private final BinaryResolver binaryResolver;
    private final ReleaseChecker releaseChecker;
    private final BinaryUpdater binaryUpdater;
    private final UpdateCache updateCache;
    private final UpdateHistory updateHistory;
    private Supplier<Boolean> activeDownloadChecker;

    public YtDlpUpdateService(BinaryResolver binaryResolver, ReleaseChecker releaseChecker, BinaryUpdater binaryUpdater, UpdateCache updateCache, UpdateHistory updateHistory) {
        this.binaryResolver = binaryResolver;
        this.releaseChecker = releaseChecker;
        this.binaryUpdater = binaryUpdater;
        this.updateCache = updateCache != null ? updateCache : new UpdateCache();
        this.updateHistory = updateHistory != null ? updateHistory : new UpdateHistory();
    }

    public YtDlpUpdateService(BinaryResolver binaryResolver, ReleaseChecker releaseChecker, BinaryUpdater binaryUpdater) {
        this(binaryResolver, releaseChecker, binaryUpdater, new UpdateCache(), new UpdateHistory());
    }

    public YtDlpUpdateService() {
        this(new BinaryResolver(), new ReleaseChecker(), new BinaryUpdater(), new UpdateCache(), new UpdateHistory());
    }

    public void setActiveDownloadChecker(Supplier<Boolean> activeDownloadChecker) {
        this.activeDownloadChecker = activeDownloadChecker;
    }

    public boolean isDownloadActive() {
        return activeDownloadChecker != null && Boolean.TRUE.equals(activeDownloadChecker.get());
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

    public CompletableFuture<UpdateInfo> checkUpdateAsync(boolean forceFreshCheck) {
        return CompletableFuture.supplyAsync(() -> {
            String current = getCurrentVersion();
            if (!forceFreshCheck && updateCache.isCacheValidHours(24)) {
                UpdateInfo cached = updateCache.getCachedUpdateInfo(current);
                if (cached != null) {
                    LOGGER.info("Usando versión de actualización en caché desde update.json");
                    return cached;
                }
            }

            UpdateInfo freshInfo = releaseChecker.checkForUpdates(current);
            updateCache.saveCache(freshInfo);
            return freshInfo;
        });
    }

    public CompletableFuture<UpdateInfo> checkUpdateAsync() {
        return checkUpdateAsync(false);
    }

    public CompletableFuture<Boolean> updateYtDlpAsync(Consumer<String> logCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (isDownloadActive()) {
                notify(logCallback, "⚠️ No se puede actualizar yt-dlp mientras existen descargas activas en curso.");
                return false;
            }

            String ytDlpPath = binaryResolver.resolveYtDlpPath();
            String current = getCurrentVersion();
            UpdateInfo info = releaseChecker.checkForUpdates(current);
            updateCache.saveCache(info);

            notify(logCallback, "Etapa 1/4: Iniciando proceso de actualización para yt-dlp...");
            notify(logCallback, "Versión instalada: " + current);
            if (info.getLatestVersion() != null && !info.getLatestVersion().isBlank()) {
                notify(logCallback, "Última versión detectada: " + info.getLatestVersion());
            }

            // Strategy 1: Safe binary updater (GitHub download -> SHA256 check -> validate -> atomic swap -> rollback)
            notify(logCallback, "Etapa 2/4: Descargando y verificando integridad...");
            boolean success = binaryUpdater.updateBinary(ytDlpPath, info.getDownloadUrl(), info.getSha256SumsUrl(), logCallback);

            if (!success) {
                // Strategy 2: Fallback to yt-dlp -U if direct binary update fails
                notify(logCallback, "⚠️ Reemplazo directo falló, intentando respaldo con 'yt-dlp -U'...");
                success = executeYtDlpSelfUpdate(ytDlpPath, logCallback);
            }

            if (success) {
                notify(logCallback, "Etapa 4/4: ✓ Actualización completada correctamente.");
            } else {
                notify(logCallback, "❌ La actualización de yt-dlp no se pudo completar.");
            }

            updateHistory.recordUpdate(current, info.getLatestVersion(), success);
            return success;
        });
    }

    private boolean executeYtDlpSelfUpdate(String ytDlpPath, Consumer<String> logCallback) {
        try {
            ProcessBuilder pb = new ProcessBuilder(List.of(ytDlpPath, "-U"));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    notify(logCallback, line);
                    LOGGER.info("[yt-dlp update fallback] {}", line);
                }
            }

            int exitCode = process.waitFor();
            boolean ok = (exitCode == 0);
            if (ok) {
                notify(logCallback, "✓ Respaldo 'yt-dlp -U' finalizó correctamente (Código 0).");
            } else {
                notify(logCallback, "❌ Respaldo 'yt-dlp -U' falló con código de salida: " + exitCode);
            }
            return ok;
        } catch (Exception e) {
            LOGGER.error("Fallo durante respaldo de actualización yt-dlp -U", e);
            notify(logCallback, "❌ Excepción durante actualización: " + e.getMessage());
            return false;
        }
    }

    private void notify(Consumer<String> consumer, String message) {
        if (consumer != null) {
            consumer.accept(message);
        }
        LOGGER.info("[YtDlpUpdateService] {}", message);
    }
}
