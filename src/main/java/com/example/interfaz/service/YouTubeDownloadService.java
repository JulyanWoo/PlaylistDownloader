package com.example.interfaz.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.exception.DownloadException;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.download.BinaryResolver;
import com.example.interfaz.service.download.ProcessExecutor;
import com.example.interfaz.service.download.SongMetadataService;
import com.example.interfaz.service.download.YtDlpCommandBuilder;
import com.example.interfaz.util.FileUtils;
import com.example.interfaz.util.ThumbnailUtils;

public class YouTubeDownloadService implements DownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeDownloadService.class);
    static final int PLAYLIST_BATCH_SIZE = 50;
    static final long PLAYLIST_BATCH_PAUSE_SECONDS = 45L;
    static final int MAX_AUTOMATIC_BATCH_ATTEMPTS = 3;
    static final long MAX_AUTOMATIC_RETRY_SECONDS = 600L;
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private final ProgressReporter progressReporter;
    private final YtDlpCommandBuilder commandBuilder;
    private final ProcessExecutor processExecutor;
    private final SongMetadataService metadataService;
    private final ExecutorService downloadExecutor;

    private final Object downloadLock = new Object();
    private volatile CompletableFuture<Boolean> currentDownload;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public YouTubeDownloadService() {
        this(new ProgressReporter(), new YtDlpCommandBuilder(new BinaryResolver()), new ProcessExecutor(),
                new SongMetadataService());
    }

    public YouTubeDownloadService(ProgressReporter progressReporter, YtDlpCommandBuilder commandBuilder,
            ProcessExecutor processExecutor, SongMetadataService metadataService) {
        this.progressReporter = progressReporter;
        this.commandBuilder = commandBuilder;
        this.processExecutor = processExecutor;
        this.metadataService = metadataService;
        this.downloadExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "YouTubeDownloadService-Worker");
            t.setDaemon(true);
            return t;
        });

        // Diagnostic: log resolved binary paths
        BinaryResolver resolver = new BinaryResolver();
        LOGGER.info("[DIAG] yt-dlp path: {}", resolver.resolveYtDlpPath());
        LOGGER.info("[DIAG] ffmpeg path: {}", resolver.resolveFfmpegPath());
    }

    public CompletableFuture<Boolean> downloadPlaylist(String playlistUrl, String outputDirectory,
            boolean newPlaylist) {
        if (closed.get()) {
            throw new IllegalStateException("Servicio de descarga cerrado.");
        }
        synchronized (downloadLock) {
            if (isDownloading()) {
                throw new IllegalStateException("Ya existe una descarga en curso.");
            }
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            this.currentDownload = future;

            downloadExecutor.execute(() -> {
                try {
                    int startFromVideo = newPlaylist ? 1 : FileUtils.loadProgress();
                    if (!newPlaylist) {
                        notifyProgress("Reanudando la descarga desde la canción #" + startFromVideo);
                    }

                    List<String> cmd = commandBuilder.buildPlaylistCommand(playlistUrl, outputDirectory,
                            startFromVideo);
                    boolean result = executeProcess(cmd);
                    future.complete(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.completeExceptionally(
                            new DownloadException("Falla en descarga de playlist: Operación cancelada", e));
                } catch (IOException e) {
                    LOGGER.error("Error de E/S en descarga de playlist", e);
                    notifyProgress("Error: " + e.getMessage());
                    future.completeExceptionally(
                            new DownloadException("Falla en descarga de playlist: " + e.getMessage(), e));
                } catch (Exception e) {
                    future.completeExceptionally(
                            new DownloadException("Falla en descarga de playlist: " + e.getMessage(), e));
                } finally {
                    synchronized (downloadLock) {
                        if (currentDownload == future) {
                            currentDownload = null;
                        }
                    }
                }
            });
            return future;
        }
    }

    public CompletableFuture<Boolean> downloadSong(String url) {
        return downloadSong(url, FileUtils.getMusicDirectory());
    }

    @Override
    public CompletableFuture<Boolean> downloadSong(String url, String outputPath) {
        if (closed.get()) {
            throw new IllegalStateException("Servicio de descarga cerrado.");
        }
        synchronized (downloadLock) {
            if (isDownloading()) {
                throw new IllegalStateException("Ya existe una descarga activa.");
            }
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            this.currentDownload = future;

            downloadExecutor.execute(() -> {
                try {
                    boolean result = downloadSongSync(url, outputPath);
                    future.complete(result);
                } catch (DownloadException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    future.completeExceptionally(e);
                } catch (Exception e) {
                    future.completeExceptionally(
                            new DownloadException("Error descargando canción (" + url + "): " + e.getMessage(), e));
                } finally {
                    synchronized (downloadLock) {
                        if (currentDownload == future) {
                            currentDownload = null;
                        }
                    }
                }
            });
            return future;
        }
    }

    public CompletableFuture<File> downloadRawAudio(String url, String stagingDir) {
        if (closed.get()) {
            throw new IllegalStateException("Servicio de descarga cerrado.");
        }
        CompletableFuture<File> future = new CompletableFuture<>();
        downloadExecutor.execute(() -> {
            try {
                File rawFile = downloadRawAudioSync(url, stagingDir);
                future.complete(rawFile);
            } catch (DownloadException e) {
                if (e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                future.completeExceptionally(e);
            } catch (Exception e) {
                future.completeExceptionally(
                        new DownloadException("Error descargando audio crudo (" + url + "): " + e.getMessage(), e));
            }
        });
        return future;
    }

    public boolean downloadRawAudioStreaming(String url, String stagingDir, Consumer<File> onFileCompleted) {
        try {
            stopRequested.set(false);
            File stagingFolder = new File(stagingDir);
            if (!stagingFolder.exists()) {
                stagingFolder.mkdirs();
            }

            java.util.Set<String> dispatchedFiles = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
            dispatchRemainingRawFiles(stagingFolder, dispatchedFiles, onFileCompleted);

            if (!ThumbnailUtils.isPlaylistUrl(url)) {
                return executeRawBatch(url, stagingDir, null, null, null, stagingFolder, dispatchedFiles,
                        onFileCompleted);
            }

            int playlistSize = resolvePlaylistSize(url);
            if (stopRequested.get()) {
                return false;
            }
            if (playlistSize <= 0) {
                LOGGER.warn("No se pudo determinar el tamaño de la playlist; se usará una sola ejecución sin pausas por petición.");
                return executeRawBatch(url, stagingDir, null, null, null, stagingFolder, dispatchedFiles,
                        onFileCompleted);
            }

            LOGGER.info("Playlist de {} canciones: descarga en bloques de {} con pausas de {} segundos.",
                    playlistSize, PLAYLIST_BATCH_SIZE, PLAYLIST_BATCH_PAUSE_SECONDS);
            for (int batchStart = 1; batchStart <= playlistSize && !stopRequested.get(); batchStart += PLAYLIST_BATCH_SIZE) {
                int batchEnd = Math.min(batchStart + PLAYLIST_BATCH_SIZE - 1, playlistSize);
                int dispatchedBeforeBatch = dispatchedFiles.size();
                notifyProgress("BATCH_START:" + batchStart + ":" + batchEnd + ":" + playlistSize);

                boolean batchSuccess = executeRawBatch(url, stagingDir, batchStart, batchEnd, playlistSize, stagingFolder,
                        dispatchedFiles, onFileCompleted);
                if (!batchSuccess) {
                    return false;
                }

                boolean downloadedFiles = dispatchedFiles.size() > dispatchedBeforeBatch;
                boolean hasNextBatch = batchEnd < playlistSize;
                if (hasNextBatch && downloadedFiles) {
                    int nextStart = batchEnd + 1;
                    notifyProgress("BATCH_PAUSE:" + PLAYLIST_BATCH_PAUSE_SECONDS + ":" + nextStart + ":" + playlistSize);
                    LOGGER.info("Bloque {}-{} completado. Pausa de {} segundos antes del siguiente bloque.",
                            batchStart, batchEnd, PLAYLIST_BATCH_PAUSE_SECONDS);
                    waitInterruptibly(PLAYLIST_BATCH_PAUSE_SECONDS);
                }
            }

            dispatchRemainingRawFiles(stagingFolder, dispatchedFiles, onFileCompleted);
            return !stopRequested.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Descarga de audio crudo cancelada por interrupción");
            notifyProgress("Error: Descarga cancelada");
            throw new DownloadException("Descarga cancelada por interrupción", e);
        } catch (IOException e) {
            LOGGER.error("Error de E/S durante la descarga de audio crudo", e);
            notifyProgress("Error: " + e.getMessage());
            throw new DownloadException("Error de E/S al ejecutar proceso de descarga: " + e.getMessage(), e);
        }
    }

    private boolean executeRawBatch(String url, String stagingDir, Integer playlistStart, Integer playlistEnd,
            Integer playlistTotal, File stagingFolder, java.util.Set<String> dispatchedFiles,
            Consumer<File> onFileCompleted)
            throws IOException, InterruptedException {
        int attempt = 0;
        while (!stopRequested.get() && attempt < MAX_AUTOMATIC_BATCH_ATTEMPTS) {
            attempt++;
            java.util.concurrent.atomic.AtomicReference<String> currentDestination = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicBoolean rateLimited = new java.util.concurrent.atomic.AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicBoolean batchTraversalCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicBoolean permanentSourceError = new java.util.concurrent.atomic.AtomicBoolean(false);
            AtomicInteger errorLines = new AtomicInteger();
            AtomicInteger unavailableItems = new AtomicInteger();
            Consumer<String> rawLineListener = line -> {
                if (line == null) return;
                String trimmed = line.trim();
                String normalized = trimmed.toLowerCase(java.util.Locale.ROOT);

                if (isRateLimitMessage(normalized)) {
                    rateLimited.set(true);
                }
                if (normalized.contains("finished downloading playlist")) {
                    batchTraversalCompleted.set(true);
                }
                if (normalized.startsWith("error:")) {
                    errorLines.incrementAndGet();
                }
                if (isUnavailableItemMessage(normalized)) {
                    unavailableItems.incrementAndGet();
                }
                if (isPermanentSourceError(normalized)) {
                    permanentSourceError.set(true);
                }

                if (trimmed.startsWith("[download] Destination:")) {
                    String pathStr = trimmed.substring("[download] Destination:".length()).trim();
                    currentDestination.set(pathStr);
                } else if (trimmed.startsWith("[download]") && trimmed.contains("has already been downloaded")) {
                    String pathStr = trimmed.replace("[download]", "").replace("has already been downloaded", "").trim();
                    checkAndDispatchFile(pathStr, dispatchedFiles, onFileCompleted);
                } else if (trimmed.startsWith("[download]") && trimmed.contains("100%")) {
                    String current = currentDestination.get();
                    if (current != null) {
                        checkAndDispatchFile(current, dispatchedFiles, onFileCompleted);
                    }
                }

                if (playlistStart != null && playlistTotal != null) {
                    progressReporter.processDownloadLine(line, playlistStart - 1, playlistTotal);
                } else {
                    progressReporter.processDownloadLine(line);
                }
            };

            boolean processSuccess = processExecutor.execute(
                    this.commandBuilder.buildRawAudioDownloadCommand(url, stagingDir, null, playlistStart, playlistEnd),
                    rawLineListener,
                    this::notifyProgress
            );
            dispatchRemainingRawFiles(stagingFolder, dispatchedFiles, onFileCompleted);

            if (processSuccess) {
                return true;
            }
            if (stopRequested.get()) {
                return false;
            }
            int retryableErrors = Math.max(0, errorLines.get() - unavailableItems.get());
            if (batchTraversalCompleted.get() && !rateLimited.get() && retryableErrors == 0) {
                if (unavailableItems.get() > 0) {
                    LOGGER.warn("Bloque {}-{} completado omitiendo {} video(s) no disponible(s); la descarga continuará automáticamente.",
                            playlistStart, playlistEnd, unavailableItems.get());
                } else {
                    LOGGER.info("yt-dlp devolvió código de advertencia después de recorrer completamente el bloque {}-{}; se continúa con el siguiente bloque.",
                            playlistStart, playlistEnd);
                }
                return true;
            }
            if (permanentSourceError.get()) {
                LOGGER.error("La fuente de YouTube no puede recuperarse automáticamente: {}", url);
                return false;
            }

            if (attempt >= MAX_AUTOMATIC_BATCH_ATTEMPTS) {
                LOGGER.error("El bloque {}-{} no pudo completarse después de {} intentos automáticos. No se harán más reintentos.",
                        playlistStart, playlistEnd, MAX_AUTOMATIC_BATCH_ATTEMPTS);
                notifyProgress("AUTOMATIC_RETRY_EXHAUSTED:" + MAX_AUTOMATIC_BATCH_ATTEMPTS);
                return false;
            }

            long waitSeconds = automaticRetryDelaySeconds(attempt, rateLimited.get());
            if (rateLimited.get()) {
                notifyProgress("RATE_LIMIT_RETRY:" + waitSeconds);
                LOGGER.warn("YouTube limitó temporalmente la descarga. Reintento automático #{} en {} segundos; el progreso se conserva.",
                        attempt + 1, waitSeconds);
            } else {
                notifyProgress("AUTOMATIC_RETRY:" + waitSeconds + ":" + (attempt + 1));
                LOGGER.warn("El bloque {}-{} quedó incompleto. Reintento automático #{} en {} segundos; el progreso se conserva.",
                        playlistStart, playlistEnd, attempt + 1, waitSeconds);
            }
            waitInterruptibly(waitSeconds);
        }
        return false;
    }

    private boolean isRateLimitMessage(String normalizedLine) {
        return normalizedLine.contains("rate-limited")
                || normalizedLine.contains("rate limit")
                || normalizedLine.contains("http error 429")
                || normalizedLine.contains("too many requests");
    }

    private boolean isUnavailableItemMessage(String normalizedLine) {
        return normalizedLine.startsWith("error:")
                && (normalizedLine.contains("video unavailable")
                    || normalizedLine.contains("private video")
                    || normalizedLine.contains("members-only")
                    || normalizedLine.contains("this video is not available")
                    || normalizedLine.contains("not available in your country")
                    || normalizedLine.contains("has been removed")
                    || normalizedLine.contains("uploader has closed")
                    || normalizedLine.contains("account has been terminated")
                    || normalizedLine.contains("has been terminated")
                    || normalizedLine.contains("join this channel")
                    || normalizedLine.contains("age-restricted"));
    }

    private boolean isPermanentSourceError(String normalizedLine) {
        return normalizedLine.contains("playlist does not exist")
                || normalizedLine.contains("this playlist is private")
                || normalizedLine.contains("unsupported url")
                || normalizedLine.contains("invalid url");
    }

    static long automaticRetryDelaySeconds(int failedAttempt, boolean rateLimited) {
        if (rateLimited) {
            if (failedAttempt == 1) return 30L;
            if (failedAttempt == 2) return 120L;
            if (failedAttempt == 3) return 300L;
            return MAX_AUTOMATIC_RETRY_SECONDS;
        }
        long multiplier = 1L << Math.min(Math.max(failedAttempt - 1, 0), 5);
        return Math.min(15L * multiplier, 300L);
    }

    private int resolvePlaylistSize(String playlistUrl) throws IOException, InterruptedException {
        AtomicInteger count = new AtomicInteger();
        boolean success = processExecutor.execute(
                commandBuilder.buildPlaylistVideoIdCommand(playlistUrl),
                line -> {
                    if (line != null && VIDEO_ID.matcher(line.trim()).matches()) {
                        count.incrementAndGet();
                    }
                },
                null);
        if (!success) {
            return 0;
        }
        return count.get();
    }

    protected void waitInterruptibly(long seconds) throws InterruptedException {
        long remainingMillis = seconds * 1000L;
        while (remainingMillis > 0 && !stopRequested.get()) {
            long slice = Math.min(remainingMillis, 1000L);
            Thread.sleep(slice);
            remainingMillis -= slice;
        }
    }

    private void dispatchRemainingRawFiles(File stagingFolder, java.util.Set<String> dispatchedFiles,
            Consumer<File> onFileCompleted) {
        File[] files = stagingFolder.listFiles((dir, name) -> name.startsWith("raw_"));
            if (files != null) {
                for (File f : files) {
                    if (dispatchedFiles.add(f.getAbsolutePath())) {
                        if (onFileCompleted != null) {
                            onFileCompleted.accept(f);
                        }
                    }
                }
            }
    }

    private void checkAndDispatchFile(String pathStr, java.util.Set<String> dispatchedFiles, Consumer<File> onFileCompleted) {
        if (pathStr == null || pathStr.isBlank()) return;
        File file = new File(pathStr);
        if (file.exists() && file.length() > 0) {
            if (dispatchedFiles.add(file.getAbsolutePath())) {
                LOGGER.info("Archivo de audio crudo completado detectado: {}", file.getName());
                if (onFileCompleted != null) {
                    onFileCompleted.accept(file);
                }
            }
        }
    }

    public File downloadRawAudioSync(String url, String stagingDir) {
        File stagingFolder = new File(stagingDir);
        if (!stagingFolder.exists()) {
            stagingFolder.mkdirs();
        }

        java.util.concurrent.atomic.AtomicReference<File> firstFile = new java.util.concurrent.atomic.AtomicReference<>();
        boolean success = downloadRawAudioStreaming(url, stagingDir, f -> {
            firstFile.compareAndSet(null, f);
        });

        if (!success && firstFile.get() == null) {
            throw new DownloadException("yt-dlp finalizó con código de error al descargar audio crudo: " + url);
        }

        File result = firstFile.get();
        if (result != null && result.exists()) {
            return result;
        }

        File[] matches = stagingFolder.listFiles((dir, name) -> name.startsWith("raw_"));
        if (matches != null && matches.length > 0) {
            return matches[0];
        }

        throw new DownloadException("No se encontró el archivo de audio crudo descargado en: " + stagingDir);
    }

    public boolean downloadSongSync(String url, String outputPath) {
        try {
            String outputDir = (outputPath == null || outputPath.trim().isEmpty()) ? FileUtils.getMusicDirectory()
                    : outputPath;
            List<String> cmd = commandBuilder.buildSingleSongCommand(url, outputDir);
            return executeProcess(cmd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Descarga cancelada por interrupción");
            notifyProgress("Error: Descarga cancelada");
            throw new DownloadException("Descarga cancelada por interrupción", e);
        } catch (IOException e) {
            LOGGER.error("Error de E/S durante la descarga de canción", e);
            notifyProgress("Error: " + e.getMessage());
            throw new DownloadException("Error de E/S al ejecutar proceso de descarga: " + e.getMessage(), e);
        }
    }

    private boolean executeProcess(List<String> cmd) throws IOException, InterruptedException {
        return processExecutor.execute(
                cmd,
                progressReporter::processDownloadLine,
                this::notifyProgress);
    }

    @Override
    public void pauseDownload() {
        processExecutor.pause();
        notifyProgress("Descarga pausada");
        LOGGER.info("Descarga pausada");
    }

    @Override
    public void resumeDownload() {
        processExecutor.resume();
        notifyProgress("Descarga reanudada");
        LOGGER.info("Descarga reanudada");
    }

    @Override
    public void stopDownload() {
        stopRequested.set(true);
        CompletableFuture<Boolean> future;
        synchronized (downloadLock) {
            future = currentDownload;
            currentDownload = null;
        }

        processExecutor.stop();

        if (future != null) {
            future.cancel(true);
        }
        notifyProgress("Descarga detenida");
        LOGGER.info("Proceso de descarga y subprocesos terminados forzosamente");
    }

    public boolean isPaused() {
        return processExecutor.isPaused();
    }

    public boolean isDownloading() {
        return processExecutor.isAlive() || (currentDownload != null && !currentDownload.isDone());
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressReporter.setProgressCallback(callback);
    }

    private void notifyProgress(String message) {
        progressReporter.notifyProgress(message);
    }

    @Override
    public boolean canHandle(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String lowerHost = host.toLowerCase();
            return lowerHost.equals("youtube.com") || lowerHost.endsWith(".youtube.com")
                    || lowerHost.equals("youtu.be") || lowerHost.endsWith(".youtu.be");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Song getSongInfo(String url) {
        return metadataService.getSongInfo(url);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        stopDownload();
        downloadExecutor.shutdownNow();
        try {
            processExecutor.close();
        } catch (Exception e) {
            LOGGER.warn("Error al cerrar ProcessExecutor: {}", e.getMessage());
        }
        LOGGER.info("YouTubeDownloadService destruido y Executor liberado");
    }
}
