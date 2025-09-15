package com.example.interfaz.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase modelo para representar una canción
 * Arquitectura de 3 capas: Capa de Datos/Modelo
 */
public class Song {
    
    private String title;
    private String artist;
    private String fileName;
    private String filePath;
    private String url;
    private long fileSize;
    private LocalDateTime downloadDate;
    private boolean isDownloaded;
    
    public Song() {
        this.downloadDate = LocalDateTime.now();
        this.isDownloaded = false;
    }
    
    public Song(String title) {
        this();
        this.title = title;
        extractArtistFromTitle();
    }
    
    public Song(String title, String artist, String fileName, String filePath, String url) {
        this();
        this.title = title;
        this.artist = artist;
        this.fileName = fileName;
        this.filePath = filePath;
        this.url = url;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        extractArtistFromTitle();
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public LocalDateTime getDownloadDate() {
        return downloadDate;
    }
    
    public void setDownloadDate(LocalDateTime downloadDate) {
        this.downloadDate = downloadDate;
    }
    
    public boolean isDownloaded() {
        return isDownloaded;
    }
    
    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }
    
    
    /**
     * Extrae el artista del título si está en formato "Artista - Canción"
     */
    private void extractArtistFromTitle() {
        if (title != null && title.contains(" - ") && artist == null) {
            String[] parts = title.split(" - ", 2);
            if (parts.length == 2) {
                this.artist = parts[0].trim();
            }
        }
    }
    
    /**
     * Obtiene el nombre de la canción sin el artista
     * @return nombre de la canción
     */
    public String getSongName() {
        if (title != null && title.contains(" - ")) {
            String[] parts = title.split(" - ", 2);
            if (parts.length == 2) {
                return parts[1].trim();
            }
        }
        return title;
    }
    
    /**
     * Obtiene el título normalizado para comparaciones
     * @return título normalizado
     */
    public String getNormalizedTitle() {
        if (title == null) return "";
        
        return title.toLowerCase()
                   .replaceAll("[\\[\\](){}]", "")
                   .replaceAll("\\s*(official|video|lyrics|audio|hd|4k)\\s*", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    /**
     * Obtiene el tamaño del archivo en formato legible
     * @return tamaño formateado
     */
    public String getFormattedFileSize() {
        if (fileSize <= 0) return "Desconocido";
        
        double size = fileSize;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(getNormalizedTitle(), song.getNormalizedTitle());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getNormalizedTitle());
    }
    
    @Override
    public String toString() {
        return "Song{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", fileName='" + fileName + '\'' +
                ", isDownloaded=" + isDownloaded +
                '}';
    }
    
    /**
     * Representación simple para mostrar en listas
     * @return representación simple
     */
    public String toDisplayString() {
        if (artist != null && !artist.isEmpty()) {
            return artist + " - " + getSongName();
        }
        return title != null ? title : "Canción sin título";
    }
}