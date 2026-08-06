package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import com.example.interfaz.service.download.BinaryResolver;
import com.example.interfaz.service.download.YtDlpCommandBuilder;
import com.example.interfaz.util.FileUtils;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class YouTubeDownloadService implements DownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeDownloadService.class);

    private final ProgressReporter progressReporter;
    private final YtDlpCommandBuilder commandBuilder;
    private final Object pauseLock = new Object();

    private Process currentProcess;
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    private static YouTubeDownloadService instance;

    public YouTubeDownloadService() {
        this(new ProgressReporter(), new YtDlpCommandBuilder(new BinaryResolver()));
    }

    public YouTubeDownloadService(ProgressReporter progressReporter, YtDlpCommandBuilder commandBuilder) {
        this.progressReporter = progressReporter;
        this.commandBuilder = commandBuilder;
    }

    public static synchronized YouTubeDownloadService getInstance() {
        if (instance == null) {
            instance = new YouTubeDownloadService();
        }
        return instance;
    }

    public CompletableFuture<Boolean> downloadPlaylist(String playlistUrl, String outputDirectory, boolean newPlaylist) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int startFromVideo = newPlaylist ? 1 : FileUtils.loadProgress();
                if (!newPlaylist) {
                    notifyProgress("Reanudando la descarga desde la canción #" + startFromVideo);
                }

                List<String> cmd = commandBuilder.buildPlaylistCommand(playlistUrl, outputDirectory, startFromVideo);
                return executeProcess(cmd);
            } catch (Exception e) {
                LOGGER.error("Error durante la descarga de playlist", e);
                notifyProgress("Error: " + e.getMessage());
                return false;
            } finally {
                resetDownloadState();
            }
        });
    }

    public CompletableFuture<Boolean> downloadSong(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> cmd = commandBuilder.buildSingleSongCommand(url, FileUtils.getMusicDirectory());
                return executeProcess(cmd);
            } catch (Exception e) {
                LOGGER.error("Error durante la descarga de canción", e);
                notifyProgress("Error: " + e.getMessage());
                return false;
            } finally {
                resetDownloadState();
            }
        });
    }

    private boolean executeProcess(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);

        currentProcess = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !shouldStop.get()) {
                handlePauseState();
                if (shouldStop.get()) {
                    break;
                }
                notifyProgress(line);
                processDownloadLine(line);
            }
        }

        int exitCode = currentProcess.waitFor();
        boolean success = exitCode == 0 && !shouldStop.get();

        if (success) {
            notifyProgress("Descarga completada exitosamente");
        } else if (shouldStop.get()) {
            notifyProgress("Descarga cancelada por el usuario");
        } else {
            notifyProgress("Error en la descarga (código: " + exitCode + ")");
        }

        return success;
    }

    public void pauseDownload() {
        this.isPaused.set(true);
        notifyProgress("Descarga pausada");
        LOGGER.info("Descarga pausada");
    }

    public void resumeDownload() {
        this.isPaused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        notifyProgress("Descarga reanudada");
        LOGGER.info("Descarga reanudada");
    }

    public void stopDownload() {
        this.shouldStop.set(true);
        this.isPaused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        if (currentProcess != null && currentProcess.isAlive()) {
            try {
                currentProcess.descendants().forEach(ProcessHandle::destroyForcibly);
            } catch (Exception e) {
                LOGGER.warn("Error cancelando subprocesos hijos: {}", e.getMessage());
            }
            currentProcess.destroyForcibly();
            notifyProgress("Descarga detenida");
            LOGGER.info("Proceso de descarga y subprocesos hijos terminados forzosamente");
        }
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public boolean shouldStop() {
        return shouldStop.get();
    }

    public boolean isDownloading() {
        return currentProcess != null && currentProcess.isAlive();
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressReporter.setProgressCallback(callback);
    }

    private void processDownloadLine(String line) {
        progressReporter.processDownloadLine(line);
    }

    private void handlePauseState() {
        synchronized (pauseLock) {
            while (isPaused.get() && !shouldStop.get()) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    shouldStop.set(true);
                    break;
                }
            }
        }
    }

    private void resetDownloadState() {
        this.currentProcess = null;
        this.isPaused.set(false);
        this.shouldStop.set(false);
    }

    private void notifyProgress(String message) {
        progressReporter.notifyProgress(message);
    }

    @Override
    public Task<Void> downloadSong(String url, String outputPath) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String outputDir = (outputPath == null || outputPath.trim().isEmpty()) ? FileUtils.getMusicDirectory() : outputPath;
                    List<String> cmd = commandBuilder.buildSingleSongCommand(url, outputDir);
                    executeProcess(cmd);
                } catch (Exception e) {
                    LOGGER.error("Error durante la descarga de canción vía Task", e);
                    notifyProgress("Error: " + e.getMessage());
                    throw e;
                } finally {
                    resetDownloadState();
                }
                return null;
            }
        };
    }

    @Override
    public boolean canHandle(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }

    @Override
    public Song getSongInfo(String url) {
        Song song = new Song();
        song.setUrl(url);
        song.setTitle("Canción desde " + url);
        return song;
    }

    @Override
    public void cancelAllDownloads() {
        stopDownload();
    }

    @Override
    public boolean hasActiveDownloads() {
        return isDownloading();
    }
}
