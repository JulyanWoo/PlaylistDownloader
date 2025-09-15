package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import javafx.concurrent.Task;

/**
 * Interfaz para servicios de descarga que define el contrato
 * para diferentes implementaciones de descarga de música.
 */
public interface DownloadService {
    
    /**
     * Descarga una canción desde una URL específica.
     * 
     * @param url URL de la canción a descargar
     * @param outputPath Ruta donde guardar el archivo descargado
     * @return Task que representa la operación de descarga
     */
    Task<Void> downloadSong(String url, String outputPath);
    
    /**
     * Verifica si el servicio puede manejar la URL proporcionada.
     * 
     * @param url URL a verificar
     * @return true si puede manejar la URL, false en caso contrario
     */
    boolean canHandle(String url);
    
    /**
     * Obtiene información de una canción desde su URL.
     * 
     * @param url URL de la canción
     * @return Información de la canción
     */
    Song getSongInfo(String url);
    
    /**
     * Cancela todas las descargas en progreso.
     */
    void cancelAllDownloads();
    
    /**
     * Verifica si hay descargas en progreso.
     * 
     * @return true si hay descargas activas, false en caso contrario
     */
    boolean hasActiveDownloads();
    
    /**
     * Pausa la descarga actual
     */
    void pauseDownload();
    
    /**
     * Reanuda la descarga pausada
     */
    void resumeDownload();
    
    /**
     * Detiene la descarga actual
     */
    void stopDownload();
}