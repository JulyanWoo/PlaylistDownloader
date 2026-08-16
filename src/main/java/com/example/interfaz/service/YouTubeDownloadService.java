package com.example.interfaz.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.exception.DownloadException;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.download.BinaryResolver;
import com.example.interfaz.service.download.ProcessExecutor;
import com.example.interfaz.service.download.SongMetadataService;
import com.example.interfaz.service.download.YtDlpCommandBuilder;
import com.example.interfaz.util.FileUtils;

public class YouTubeDownloadService implements DownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeDownloadService.class);

    private final ProgressReporter progressReporter;
    private final YtDlpCommandBuilder commandBuilder;
    private final ProcessExecutor processExecutor;
    private final SongMetadataService metadataService;
    private final ExecutorService downloadExecutor;

    private final Object downloadLock = new Object();
    private volatile CompletableFuture<Boolean> currentDownload;
    private final AtomicBoolean closed = new AtomicBoolean(false);

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
            File stagingFolder = new File(stagingDir);
            if (!stagingFolder.exists()) {
                stagingFolder.mkdirs();
            }

            List<String> cmd = this.commandBuilder.buildRawAudioDownloadCommand(url, stagingDir);
            java.util.Set<String> dispatchedFiles = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
            java.util.concurrent.atomic.AtomicReference<String> currentDestination = new java.util.concurrent.atomic.AtomicReference<>();

            Consumer<String> rawLineListener = line -> {
                if (line == null) return;
                String trimmed = line.trim();

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

                progressReporter.processDownloadLine(line);
            };

            boolean success = processExecutor.execute(
                    cmd,
                    rawLineListener,
                    this::notifyProgress
            );

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

            return success;
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
