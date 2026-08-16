package com.example.interfaz.service.download;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.exception.DownloadException;


public class AudioConversionService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioConversionService.class);

    private final BinaryResolver binaryResolver;
    private final ExecutorService conversionExecutor;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Set<Process> activeProcesses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AudioConversionService() {
        this(new BinaryResolver());
    }

    public AudioConversionService(BinaryResolver binaryResolver) {
        this(binaryResolver, Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "AudioConverter-Worker");
            t.setDaemon(true);
            return t;
        }));
    }

    public AudioConversionService(BinaryResolver binaryResolver, ExecutorService conversionExecutor) {
        this.binaryResolver = binaryResolver;
        this.conversionExecutor = conversionExecutor;
    }

    public CompletableFuture<File> convertToMp3(
            File rawAudioFile,
            File targetMp3File,
            Consumer<String> statusCallback
    ) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AudioConversionService cerrado."));
        }

        if (rawAudioFile == null || !rawAudioFile.exists()) {
            return CompletableFuture.failedFuture(
                    new DownloadException("Archivo de audio original no existe: " + (rawAudioFile != null ? rawAudioFile.getAbsolutePath() : "null"))
            );
        }

        if (targetMp3File == null) {
            return CompletableFuture.failedFuture(new DownloadException("Archivo destino MP3 no especificado."));
        }

        CompletableFuture<File> future = new CompletableFuture<>();

        conversionExecutor.execute(() -> {
            Process process = null;
            try {
                if (statusCallback != null) {
                    statusCallback.accept("Extrayendo audio MP3...");
                }

                File parentDir = targetMp3File.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                List<String> cmd = buildFfmpegCommand(rawAudioFile, targetMp3File);
                LOGGER.info("Iniciando conversión FFmpeg: {} -> {}", rawAudioFile.getName(), targetMp3File.getName());

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                process = pb.start();
                activeProcesses.add(process);

                Process procRef = process;
                Thread drainer = new Thread(() -> {
                    try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(procRef.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            LOGGER.trace("[ffmpeg] {}", line);
                        }
                    } catch (IOException ignored) {
                    }
                }, "FFmpeg-Drainer");
                drainer.setDaemon(true);
                drainer.start();

                int exitCode = process.waitFor();
                activeProcesses.remove(process);

                if (exitCode == 0 && targetMp3File.exists() && targetMp3File.length() > 0) {
                    LOGGER.info("Conversión MP3 exitosa: {}", targetMp3File.getAbsolutePath());
                    // Cleanup raw temporary file
                    try {
                        Files.deleteIfExists(rawAudioFile.toPath());
                    } catch (IOException e) {
                        LOGGER.warn("No se pudo eliminar archivo temporal {}: {}", rawAudioFile.getAbsolutePath(), e.getMessage());
                    }
                    future.complete(targetMp3File);
                } else {
                    LOGGER.error("FFmpeg falló con código de salida {} al convertir {}", exitCode, rawAudioFile.getName());
                    cleanupFiles(rawAudioFile, targetMp3File);
                    future.completeExceptionally(new DownloadException("Error en FFmpeg al convertir audio (exit code: " + exitCode + ")"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Conversión de audio cancelada por interrupción");
                if (process != null) {
                    process.destroyForcibly();
                    activeProcesses.remove(process);
                }
                cleanupFiles(rawAudioFile, targetMp3File);
                future.completeExceptionally(new DownloadException("Conversión de audio cancelada", e));
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Error durante conversión a MP3", e);
                if (process != null) {
                    process.destroyForcibly();
                    activeProcesses.remove(process);
                }
                cleanupFiles(rawAudioFile, targetMp3File);
                future.completeExceptionally(new DownloadException("Error en conversión a MP3: " + e.getMessage(), e));
            }
        });

        return future;
    }

    public List<String> buildFfmpegCommand(File rawAudioFile, File targetMp3File) {
        String ffmpegPath = binaryResolver.resolveFfmpegPath();
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(rawAudioFile.getAbsolutePath());
        cmd.add("-vn");
        cmd.add("-acodec");
        cmd.add("libmp3lame");
        cmd.add("-q:a");
        cmd.add("2");
        cmd.add(targetMp3File.getAbsolutePath());
        return cmd;
    }

    public void cancelAll() {
        for (Process p : activeProcesses) {
            try {
                if (p.isAlive()) {
                    ProcessExecutor.killProcessTree(p);
                }
            } catch (Exception e) {
                LOGGER.warn("Error al forzar detención de FFmpeg: {}", e.getMessage());
            }
        }
        activeProcesses.clear();
        LOGGER.info("Todas las conversiones activas de audio fueron canceladas.");
    }

    private void cleanupFiles(File rawFile, File targetFile) {
        try {
            if (rawFile != null && rawFile.exists()) {
                Files.deleteIfExists(rawFile.toPath());
            }
        } catch (IOException ignored) {
        }
        try {
            if (targetFile != null && targetFile.exists() && targetFile.length() == 0) {
                Files.deleteIfExists(targetFile.toPath());
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        cancelAll();
        conversionExecutor.shutdownNow();
        LOGGER.info("AudioConversionService destruido y recursos liberados.");
    }
}
