package com.example.interfaz.service;

import com.example.interfaz.exception.DownloadException;
import com.example.interfaz.model.Song;
import com.example.interfaz.service.download.BinaryResolver;
import com.example.interfaz.service.download.ProcessExecutor;
import com.example.interfaz.service.download.SongMetadataService;
import com.example.interfaz.service.download.YtDlpCommandBuilder;
import com.example.interfaz.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
        this(new ProgressReporter(), new YtDlpCommandBuilder(new BinaryResolver()), new ProcessExecutor(), new SongMetadataService());
    }

    public YouTubeDownloadService(ProgressReporter progressReporter, YtDlpCommandBuilder commandBuilder, ProcessExecutor processExecutor, SongMetadataService metadataService) {
        this.progressReporter = progressReporter;
        this.commandBuilder = commandBuilder;
        this.processExecutor = processExecutor;
        this.metadataService = metadataService;
        this.downloadExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "YouTubeDownloadService-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Boolean> downloadPlaylist(String playlistUrl, String outputDirectory, boolean newPlaylist) {
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

                    List<String> cmd = commandBuilder.buildPlaylistCommand(playlistUrl, outputDirectory, startFromVideo);
                    boolean result = executeProcess(cmd);
                    future.complete(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.completeExceptionally(new DownloadException("Falla en descarga de playlist: Operación cancelada", e));
                } catch (IOException e) {
                    LOGGER.error("Error de E/S en descarga de playlist", e);
                    notifyProgress("Error: " + e.getMessage());
                    future.completeExceptionally(new DownloadException("Falla en descarga de playlist: " + e.getMessage(), e));
                } catch (Exception e) {
                    future.completeExceptionally(new DownloadException("Falla en descarga de playlist: " + e.getMessage(), e));
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
                    future.completeExceptionally(new DownloadException("Error descargando canción (" + url + "): " + e.getMessage(), e));
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

    public boolean downloadSongSync(String url, String outputPath) {
        try {
            String outputDir = (outputPath == null || outputPath.trim().isEmpty()) ? FileUtils.getMusicDirectory() : outputPath;
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
            this::notifyProgress
        );
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
