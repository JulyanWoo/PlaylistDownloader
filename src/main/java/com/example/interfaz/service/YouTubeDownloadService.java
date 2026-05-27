
package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import com.example.interfaz.util.FileUtils;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;
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

    private static File getJarFolder() {
        try {
            return new File(YouTubeDownloadService.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getYtDlpPath() {
        String env = System.getenv(YT_DLP_ENV);
        if (env != null && !env.isEmpty() && new File(env).exists()) return env;
        
        // 1. Check relative to user.dir
        Path relative = Paths.get(System.getProperty("user.dir"), DEFAULT_YT_DLP_RELATIVE);
        if (Files.exists(relative)) return relative.toString();
        
        // 2. Check relative to JAR location (useful when launched from shortcuts)
        File jarFolder = getJarFolder();
        if (jarFolder != null) {
            File jarRelative = new File(jarFolder, DEFAULT_YT_DLP_RELATIVE);
            if (jarRelative.exists()) return jarRelative.getAbsolutePath();
            
            File jarParentRelative = new File(jarFolder.getParentFile(), DEFAULT_YT_DLP_RELATIVE);
            if (jarParentRelative.exists()) return jarParentRelative.getAbsolutePath();
        }
        
        Path srcMainRelative = Paths.get(System.getProperty("user.dir"), "src", "main", "Libs", getYtDlpExecutableName());
        if (Files.exists(srcMainRelative)) return srcMainRelative.toString();
        String underSrc = findInDir(Paths.get(System.getProperty("user.dir"), "src", "main", "Libs"), getYtDlpExecutableName());
        if (underSrc != null) return underSrc;
        String exe = getYtDlpExecutableName();
        String found = findInPath(exe);
        if (found != null) return found;
        return exe;
    }

    private static String getFfmpegPath() {
        String env = System.getenv(FFMPEG_ENV);
        if (env != null && !env.isEmpty() && new File(env).exists()) return env;
        
        // 1. Check relative to user.dir
        Path relative = Paths.get(System.getProperty("user.dir"), DEFAULT_FFMPEG_RELATIVE);
        if (Files.exists(relative)) return relative.toString();
        
        // 2. Check relative to JAR location
        File jarFolder = getJarFolder();
        if (jarFolder != null) {
            File jarRelative = new File(jarFolder, DEFAULT_FFMPEG_RELATIVE);
            if (jarRelative.exists()) return jarRelative.getAbsolutePath();
            
            File jarParentRelative = new File(jarFolder.getParentFile(), DEFAULT_FFMPEG_RELATIVE);
            if (jarParentRelative.exists()) return jarParentRelative.getAbsolutePath();
        }
        
        Path srcMainLibs = Paths.get(System.getProperty("user.dir"), "src", "main", "Libs");
        String foundLocal = findInDir(srcMainLibs, getFfmpegExecutableName());
        if (foundLocal != null) return foundLocal;
        String exe = getFfmpegExecutableName();
        String found = findInPath(exe);
        if (found != null) return found;
        return exe;
    }
    
    private static String getYtDlpExecutableName() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "yt-dlp.exe" : "yt-dlp";
    }
    
    private static String getFfmpegExecutableName() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
    }
    
    private static String findInPath(String exe) {
        String path = System.getenv("PATH");
        if (path == null || path.isEmpty()) return null;
        String[] dirs = path.split(File.pathSeparator);
        for (String d : dirs) {
            File f = new File(d, exe);
            if (f.exists() && f.isFile()) return f.getAbsolutePath();
        }
        return null;
    }
    
    private static String findInDir(Path dir, String exe) {
        if (dir == null || !Files.exists(dir)) return null;
        try (var stream = Files.walk(dir, 4)) {
            var opt = stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().equalsIgnoreCase(exe)).findFirst();
            return opt.map(Path::toString).orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
    
    private static boolean isExistingPath(String p) {
        if (p == null || p.isEmpty()) return false;
        return new File(p).exists();
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
                List<String> cmd = new ArrayList<>();
                cmd.add(getYtDlpPath());
                cmd.add("-x");
                cmd.add("--audio-format");
                cmd.add("mp3");
                String ffmpegPath = getFfmpegPath();
                if (isExistingPath(ffmpegPath)) {
                    cmd.add("--ffmpeg-location");
                    cmd.add(ffmpegPath);
                }
                cmd.add("-o");
                cmd.add(Paths.get(outputDirectory, "%(title)s.%(ext)s").toString());
                cmd.add("--playlist-start");
                cmd.add(String.valueOf(startFromVideo));
                cmd.add("--no-overwrites");
                cmd.add(playlistUrl);
                processBuilder.command(cmd);
                processBuilder.redirectErrorStream(true);
                
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
                List<String> cmd = new ArrayList<>();
                cmd.add(getYtDlpPath());
                cmd.add("-x");
                cmd.add("--audio-format");
                cmd.add("mp3");
                String ffmpegPath = getFfmpegPath();
                if (isExistingPath(ffmpegPath)) {
                    cmd.add("--ffmpeg-location");
                    cmd.add(ffmpegPath);
                }
                cmd.add("-o");
                cmd.add(FileUtils.getMusicDirectory() + File.separator + "%(title)s.%(ext)s");
                cmd.add("--no-overwrites");
                cmd.add(url);
                processBuilder.command(cmd);
                
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
                    List<String> cmd = new ArrayList<>();
                    cmd.add(getYtDlpPath());
                    cmd.add("-x");
                    cmd.add("--audio-format");
                    cmd.add("mp3");
                    String ffmpegPath = getFfmpegPath();
                    if (isExistingPath(ffmpegPath)) {
                        cmd.add("--ffmpeg-location");
                        cmd.add(ffmpegPath);
                    }
                    cmd.add("-o");
                    cmd.add(outputDir + File.separator + "%(title)s.%(ext)s");
                    cmd.add("--no-overwrites");
                    cmd.add(url);
                    processBuilder.command(cmd);
                    processBuilder.redirectErrorStream(true);

                    
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
