package com.example.interfaz.util;

import com.example.interfaz.model.Song;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    private static String musicDirectory = null;

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

    private FileUtils() {
        throw new UnsupportedOperationException("Esta es una clase de utilidades");
    }

    public static Set<String> loadDownloadedSongs() {
        Set<String> songs = new HashSet<>();
        File file = new File(getDownloadedSongsFile());

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        songs.add(line.trim());
                    }
                }
                LOGGER.info("Cargadas " + songs.size() + " canciones desde archivo");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error al cargar canciones descargadas", e);
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

        try {

            createDirectoryIfNotExists(getMusicDirectory());

            try (FileWriter writer = new FileWriter(getDownloadedSongsFile(), true)) {
                writer.write(songTitle.trim() + "\n");
                writer.flush();
                LOGGER.info("Canción guardada: " + songTitle);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar canción: " + songTitle, e);
        }
    }

    public static void saveDownloadedSong(Song song) {
        if (song != null && song.getTitle() != null) {
            saveDownloadedSong(song.getTitle());
        }
    }

    public static int loadProgress() {
        File file = new File(getProgressFile());

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    int progress = Integer.parseInt(line.trim());
                    LOGGER.info("Progreso cargado: " + progress);
                    return progress;
                }
            } catch (IOException | NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Error al cargar progreso", e);
            }
        }

        return 1; 
    }

    public static void saveProgress(int videoNumber) {
        try {
            createDirectoryIfNotExists(getMusicDirectory());

            try (FileWriter writer = new FileWriter(getProgressFile())) {
                writer.write(String.valueOf(videoNumber));
                writer.flush();
                LOGGER.fine("Progreso guardado: " + videoNumber);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar progreso", e);
        }
    }

    public static void createDirectoryIfNotExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                LOGGER.info("Directorio creado: " + directoryPath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al crear directorio: " + directoryPath, e);
        }
    }

    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : -1;
    }

    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    LOGGER.info("Archivo eliminado: " + filePath);
                }
                return deleted;
            }
            return true; 
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar archivo: " + filePath, e);
            return false;
        }
    }

    public static void clearDownloadedSongsFile() {
        try {
            createDirectoryIfNotExists(getMusicDirectory());

            File file = new File(getDownloadedSongsFile());
            if (file.exists()) {
                file.delete();
                LOGGER.info("Archivo de canciones descargadas limpiado");
            }

            try (FileWriter writer = new FileWriter(getDownloadedSongsFile())) {
                writer.write(""); 
                writer.flush();
                LOGGER.info("Archivo de canciones descargadas reinicializado");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al limpiar archivo de canciones descargadas", e);
        }
    }

    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        filePath = filePath.replace('/', '\\');

        int lastSeparator = filePath.lastIndexOf('\\');
        if (lastSeparator >= 0 && lastSeparator < filePath.length() - 1) {
            return filePath.substring(lastSeparator + 1);
        }

        return filePath;
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

        title = title.replaceAll("[_\\-]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

        return title;
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
