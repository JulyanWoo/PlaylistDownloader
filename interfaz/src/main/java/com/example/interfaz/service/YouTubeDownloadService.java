
package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import com.example.interfaz.util.FileUtils;
import javafx.concurrent.Task;


import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio para descarga de música desde YouTube
 * Arquitectura de 3 capas: Capa de Lógica de Negocio
 */
public class YouTubeDownloadService implements DownloadService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeDownloadService.class);

    private static final String YT_DLP_ENV = "YT_DLP_PATH";
    private static final String FFMPEG_ENV = "FFMPEG_PATH";
    private static final String DEFAULT_YT_DLP_RELATIVE = "Libs/yt-dlp.exe";
    private static final String DEFAULT_FFMPEG_RELATIVE = "Libs/ffmpeg-2024-09-26-git-f43916e217-full_build/ffmpeg-2024-09-26-git-f43916e217-full_build/bin/ffmpeg.exe";

    private static String getYtDlpPath() {
        String env = System.getenv(YT_DLP_ENV);
        if (env != null && !env.isEmpty()) return env;
        Path relative = Paths.get(System.getProperty("user.dir"), DEFAULT_YT_DLP_RELATIVE);
        return relative.toString();
    }

    private static String getFfmpegPath() {
        String env = System.getenv(FFMPEG_ENV);
        if (env != null && !env.isEmpty()) return env;
        Path relative = Paths.get(System.getProperty("user.dir"), DEFAULT_FFMPEG_RELATIVE);
        return relative.toString();
    }
    
    private final ProgressReporter progressReporter;
    
    private Process currentProcess;
    private boolean isPaused;
    private boolean shouldStop;
    
    private static YouTubeDownloadService instance;
    
    public YouTubeDownloadService() {
        this.progressReporter = new ProgressReporter();
        this.isPaused = false;
        this.shouldStop = false;
    }
    
    /**
     * Obtiene la instancia singleton del servicio
     * @return instancia del servicio
     */
    public static synchronized YouTubeDownloadService getInstance() {
        if (instance == null) {
            instance = new YouTubeDownloadService();
        }
        return instance;
    }
    
    /**
     * Descarga una playlist completa de YouTube
     * @param playlistUrl URL de la playlist
     * @param outputDirectory directorio de salida
     * @param newPlaylist true si es una nueva playlist, false para reanudar
     * @return CompletableFuture con el resultado
     */
    public CompletableFuture<Boolean> downloadPlaylist(String playlistUrl, String outputDirectory, boolean newPlaylist) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int startFromVideo = 1;
                
                if (!newPlaylist) {
                    startFromVideo = FileUtils.loadProgress();
                    notifyProgress("Reanudando la descarga desde la canción #" + startFromVideo);
                }
                
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command(
                    getYtDlpPath(),
                    "-x",
                    "--audio-format", "mp3",
                    "--ffmpeg-location", getFfmpegPath(),
                    "-o", Paths.get(outputDirectory, "%(title)s.%(ext)s").toString(),
                    "--playlist-start", String.valueOf(startFromVideo),
                    "--no-overwrites",
                    playlistUrl
                );
                
                currentProcess = processBuilder.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && !shouldStop) {
                        // Manejar pausa
                        handlePauseState();
                        
                        if (shouldStop) {
                            break;
                        }
                        
                        notifyProgress(line);
                        processDownloadLine(line);
                    }
                }
                
                int exitCode = currentProcess.waitFor();
                boolean success = exitCode == 0 && !shouldStop;
                
                if (success) {
                    notifyProgress("Descarga de playlist completada exitosamente");
                } else if (shouldStop) {
                    notifyProgress("Descarga cancelada por el usuario");
                } else {
                    notifyProgress("Error en la descarga (código: " + exitCode + ")");
                }
                
                return success;
                
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error durante la descarga de playlist", e);
                notifyProgress("Error: " + e.getMessage());
                return false;
            } finally {
                resetDownloadState();
            }
        });
    }
    
    /**
     * Descarga una canción individual
     * @param url URL de la canción
     * @return CompletableFuture con el resultado
     */
    public CompletableFuture<Boolean> downloadSong(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command(
                    getYtDlpPath(),
                    "-x",
                    "--audio-format", "mp3",
                    "--ffmpeg-location", getFfmpegPath(),
                    "-o", FileUtils.getMusicDirectory() + File.separator + "%(title)s.%(ext)s",
                    "--no-overwrites",
                    url
                );
                
                currentProcess = processBuilder.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && !shouldStop) {
                        // Manejar pausa
                        handlePauseState();
                        
                        if (shouldStop) {
                            break;
                        }
                        
                        notifyProgress(line);
                        processDownloadLine(line);
                    }
                }
                
                int exitCode = currentProcess.waitFor();
                boolean success = exitCode == 0 && !shouldStop;
                
                if (success) {
                    notifyProgress("Canción descargada exitosamente");
                } else if (shouldStop) {
                    notifyProgress("Descarga cancelada");
                } else {
                    notifyProgress("Error en la descarga");
                }
                
                return success;
                
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error durante la descarga de canción", e);
                notifyProgress("Error: " + e.getMessage());
                return false;
            } finally {
                resetDownloadState();
            }
        });
    }
    
    /**
     * Pausa la descarga actual
     */
    public void pauseDownload() {
        this.isPaused = true;
        notifyProgress("Descarga pausada");
        LOGGER.info("Descarga pausada");
    }
    
    /**
     * Reanuda la descarga pausada
     */
    public void resumeDownload() {
        this.isPaused = false;
        notifyProgress("Descarga reanudada");
        LOGGER.info("Descarga reanudada");
    }
    
    /**
     * Detiene la descarga actual
     */
    public void stopDownload() {
        this.shouldStop = true;
        this.isPaused = false;
        
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            notifyProgress("Descarga detenida");
            LOGGER.info("Proceso de descarga terminado forzosamente");
        }
    }
    
    /**
     * Verifica si la descarga está pausada
     * @return true si está pausada
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Verifica si se debe detener la descarga
     * @return true si se debe detener
     */
    public boolean shouldStop() {
        return shouldStop;
    }
    
    /**
     * Verifica si hay una descarga en progreso
     * @return true si hay descarga activa
     */
    public boolean isDownloading() {
        return currentProcess != null && currentProcess.isAlive();
    }
    
    /**
     * Establece el callback para notificaciones de progreso
     * @param callback función que recibe las actualizaciones
     */
    public void setProgressCallback(Consumer<String> callback) {
        this.progressReporter.setProgressCallback(callback);
    }
    
    /**
     * Procesa una línea de salida del proceso de descarga
     * @param line línea de salida
     */
    private void processDownloadLine(String line) {
        progressReporter.processDownloadLine(line);
    }
    

    
    /**
     * Maneja el estado de pausa
     */
    private void handlePauseState() {
        while (isPaused && !shouldStop) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shouldStop = true;
                break;
            }
        }
    }
    
    /**
     * Reinicia el estado de descarga
     */
    private void resetDownloadState() {
        this.currentProcess = null;
        this.isPaused = false;
        this.shouldStop = false;
    }
    
    /**
     * Notifica progreso a través del callback
     * @param message mensaje de progreso
     */
    private void notifyProgress(String message) {
        progressReporter.notifyProgress(message);
    }
    
    @Override
    public Task<Void> downloadSong(String url, String outputPath) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    String outputDir = outputPath.isEmpty() ? FileUtils.getMusicDirectory() : outputPath;
                    processBuilder.command(
                        getYtDlpPath(),
                        "-x",
                        "--audio-format", "mp3",
                        "--ffmpeg-location", getFfmpegPath(),
                        "-o", outputDir + File.separator + "%(title)s.%(ext)s",
                        "--no-overwrites",
                        url
                    );

                    
                    currentProcess = processBuilder.start();
                    
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null && !shouldStop) {
                            handlePauseState();
                            
                            if (shouldStop) {
                                break;
                            }
                            
                            System.out.println(line);
                            notifyProgress(line);
                            processDownloadLine(line);
                        }
                    }
                    
                    int exitCode = currentProcess.waitFor();
                    boolean success = exitCode == 0 && !shouldStop;
                    
                    if (success) {
                        notifyProgress("Canción descargada exitosamente");
                    } else if (shouldStop) {
                        notifyProgress("Descarga cancelada");
                    } else {
                        notifyProgress("Error en la descarga");
                    }
                    
                } catch (IOException | InterruptedException e) {
                    LOGGER.error("Error durante la descarga de canción", e);
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