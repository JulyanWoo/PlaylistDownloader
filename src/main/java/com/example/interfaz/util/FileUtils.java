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

/**
 * Utilidades para manejo de archivos
 * Arquitectura de 3 capas: Capa de Utilidades
 */
public class FileUtils {
    
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());
    
    // Configuración dinámica de rutas
    private static String musicDirectory = null;
    
    /**
     * Obtiene el directorio de música configurado o el por defecto
     * @return ruta del directorio de música
     */
    public static String getMusicDirectory() {
        if (musicDirectory == null) {
            String userHome = System.getProperty("user.home");
            String desktop = userHome + File.separator + "Desktop";
            musicDirectory = desktop + File.separator + "MUSICA";
            createDirectoryIfNotExists(musicDirectory);
        }
        return musicDirectory;
    }
    
    /**
     * Establece un directorio de música personalizado
     * @param directory nueva ruta del directorio de música
     */
    public static void setMusicDirectory(String directory) {
        musicDirectory = directory;
        createDirectoryIfNotExists(musicDirectory);
    }
    
    /**
     * Obtiene la ruta del archivo de progreso
     * @return ruta del archivo de progreso
     */
    public static String getProgressFile() {
        return getMusicDirectory() + File.separator + "download_progress.txt";
    }
    
    /**
     * Obtiene la ruta del archivo de canciones descargadas
     * @return ruta del archivo de canciones descargadas
     */
    public static String getDownloadedSongsFile() {
        return getMusicDirectory() + File.separator + "canciones_descargadas.txt";
    }
    
    // Constructor privado para clase de utilidades
    private FileUtils() {
        throw new UnsupportedOperationException("Esta es una clase de utilidades");
    }
    
    /**
     * Carga las canciones descargadas desde el archivo
     * @return conjunto de títulos de canciones descargadas
     */
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
    
    /**
     * Guarda una canción en el archivo de canciones descargadas
     * @param songTitle título de la canción
     */
    public static void saveDownloadedSong(String songTitle) {
        if (songTitle == null || songTitle.trim().isEmpty()) {
            return;
        }
        
        try {
            // Crear directorio si no existe
            createDirectoryIfNotExists(getMusicDirectory());
            
            // Escribir al archivo
            try (FileWriter writer = new FileWriter(getDownloadedSongsFile(), true)) {
                writer.write(songTitle.trim() + "\n");
                writer.flush();
                LOGGER.info("Canción guardada: " + songTitle);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar canción: " + songTitle, e);
        }
    }
    
    /**
     * Guarda una canción usando el modelo Song
     * @param song objeto Song
     */
    public static void saveDownloadedSong(Song song) {
        if (song != null && song.getTitle() != null) {
            saveDownloadedSong(song.getTitle());
        }
    }
    
    /**
     * Carga el progreso de descarga desde el archivo
     * @return número del último video descargado
     */
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
        
        return 1; // Comenzar desde el primer video
    }
    
    /**
     * Guarda el progreso de descarga en el archivo
     * @param videoNumber número del video actual
     */
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
    
    /**
     * Crea un directorio si no existe
     * @param directoryPath ruta del directorio
     */
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
    
    /**
     * Verifica si un archivo existe
     * @param filePath ruta del archivo
     * @return true si existe, false en caso contrario
     */
    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }
    
    /**
     * Obtiene el tamaño de un archivo
     * @param filePath ruta del archivo
     * @return tamaño en bytes, -1 si no existe
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : -1;
    }
    
    /**
     * Elimina un archivo
     * @param filePath ruta del archivo
     * @return true si se eliminó correctamente
     */
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
            return true; // No existe, consideramos como eliminado
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar archivo: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Limpia el archivo de canciones descargadas y lo reinicializa
     * Esto resuelve el problema de números en lugar de nombres
     */
    public static void clearDownloadedSongsFile() {
        try {
            createDirectoryIfNotExists(getMusicDirectory());
            
            // Eliminar el archivo existente si tiene contenido incorrecto
            File file = new File(getDownloadedSongsFile());
            if (file.exists()) {
                file.delete();
                LOGGER.info("Archivo de canciones descargadas limpiado");
            }
            
            // Crear un nuevo archivo vacío
            try (FileWriter writer = new FileWriter(getDownloadedSongsFile())) {
                writer.write(""); // Archivo vacío
                writer.flush();
                LOGGER.info("Archivo de canciones descargadas reinicializado");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al limpiar archivo de canciones descargadas", e);
        }
    }
    
    /**
     * Extrae el nombre del archivo de una ruta completa
     * @param filePath ruta completa
     * @return nombre del archivo
     */
    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        // Normalizar separadores de ruta
        filePath = filePath.replace('/', '\\');
        
        int lastSeparator = filePath.lastIndexOf('\\');
        if (lastSeparator >= 0 && lastSeparator < filePath.length() - 1) {
            return filePath.substring(lastSeparator + 1);
        }
        
        return filePath;
    }
    
    /**
     * Extrae el título de la canción del nombre del archivo
     * @param fileName nombre del archivo
     * @return título de la canción
     */
    public static String extractSongTitle(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        // Remover extensión
        String title = fileName;
        int lastDot = title.lastIndexOf('.');
        if (lastDot > 0) {
            title = title.substring(0, lastDot);
        }
        
        // Limpiar caracteres especiales y normalizar
        title = title.replaceAll("[_\\-]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        
        return title;
    }
    
    /**
     * Valida si una ruta de archivo es válida
     * @param filePath ruta del archivo
     * @return true si es válida
     */
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
    
    /**
     * Obtiene la extensión de un archivo
     * @param fileName nombre del archivo
     * @return extensión (sin el punto)
     */
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