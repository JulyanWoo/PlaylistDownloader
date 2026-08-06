package com.example.interfaz.util;

import com.example.interfaz.model.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private static String musicDirectory = null;

    private FileUtils() {
        throw new UnsupportedOperationException("Esta es una clase de utilidades");
    }

    public static String getMusicDirectory() {
        if (musicDirectory == null) {
            String userHome = System.getProperty("user.home");
            String desktop = userHome + File.separator + "Desktop";
            musicDirectory = desktop + File.separator + "MUSICA";
            createDirectoryIfNotExists(musicDirectory);
        }
        return musicDirectory;
    }

    public static void setMusicDirectory(String directory) {
        musicDirectory = directory;
        createDirectoryIfNotExists(musicDirectory);
    }

    public static String getProgressFile() {
        return getMusicDirectory() + File.separator + "download_progress.txt";
    }

    public static String getDownloadedSongsFile() {
        return getMusicDirectory() + File.separator + "canciones_descargadas.txt";
    }

    public static Set<String> loadDownloadedSongs() {
        Set<String> songs = new HashSet<>();
        Path path = Paths.get(getDownloadedSongsFile());

        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        songs.add(trimmed);
                    }
                }
                LOGGER.info("Cargadas {} canciones desde archivo UTF-8", songs.size());
            } catch (IOException e) {
                LOGGER.error("Error al cargar canciones descargadas desde {}", path, e);
            }
        } else {
            LOGGER.info("Archivo de canciones no existe, se creará uno nuevo");
        }

        return songs;
    }

    public static void saveDownloadedSong(String songTitle) {
        if (songTitle == null || songTitle.trim().isEmpty()) {
            return;
        }

        String title = songTitle.trim();
        createDirectoryIfNotExists(getMusicDirectory());
        Path path = Paths.get(getDownloadedSongsFile());

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(title);
            writer.newLine();
            LOGGER.info("Canción guardada en UTF-8: {}", title);
        } catch (IOException e) {
            LOGGER.error("Error al guardar canción: {}", title, e);
        }
    }

    public static void saveDownloadedSong(Song song) {
        if (song != null && song.getTitle() != null) {
            saveDownloadedSong(song.getTitle());
        }
    }

    public static int loadProgress() {
        Path path = Paths.get(getProgressFile());

        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    int progress = Integer.parseInt(line.trim());
                    LOGGER.info("Progreso cargado: {}", progress);
                    return progress;
                }
            } catch (IOException | NumberFormatException e) {
                LOGGER.warn("Error al cargar progreso desde {}", path, e);
            }
        }

        return 1;
    }

    public static void saveProgress(int videoNumber) {
        createDirectoryIfNotExists(getMusicDirectory());
        Path path = Paths.get(getProgressFile());

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(String.valueOf(videoNumber));
            LOGGER.debug("Progreso guardado: {}", videoNumber);
        } catch (IOException e) {
            LOGGER.error("Error al guardar progreso {}", videoNumber, e);
        }
    }

    public static void createDirectoryIfNotExists(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                LOGGER.info("Directorio creado: {}", directoryPath);
            }
        } catch (IOException e) {
            LOGGER.error("Error al crear directorio: {}", directoryPath, e);
        }
    }

    public static boolean fileExists(String filePath) {
        return filePath != null && Files.exists(Paths.get(filePath));
    }

    public static long getFileSize(String filePath) {
        if (filePath == null) return -1;
        Path path = Paths.get(filePath);
        try {
            return Files.exists(path) ? Files.size(path) : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) return true;
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.error("Error al eliminar archivo: {}", filePath, e);
            return false;
        }
    }

    public static void clearDownloadedSongsFile() {
        createDirectoryIfNotExists(getMusicDirectory());
        Path path = Paths.get(getDownloadedSongsFile());
        try {
            Files.write(path, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("Archivo de canciones descargadas reinicializado en UTF-8");
        } catch (IOException e) {
            LOGGER.error("Error al limpiar archivo de canciones descargadas", e);
        }
    }

    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "";
        }
        Path path = Paths.get(filePath);
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "";
    }

    public static String extractSongTitle(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        String title = fileName;
        int lastDot = title.lastIndexOf('.');
        if (lastDot > 0) {
            title = title.substring(0, lastDot);
        }

        return title.replaceAll("[_\\-]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
    }

    public static boolean isValidFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        try {
            Paths.get(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }

        return "";
    }
}
