package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import java.util.List;

/**
 * Interfaz para servicios de filtrado que define el contrato
 * para diferentes estrategias de filtrado de canciones.
 */
public interface FilterService {
    
    /**
     * Filtra una lista de canciones eliminando duplicados.
     * 
     * @param songs Lista de canciones a filtrar
     * @return Lista de canciones sin duplicados
     */
    List<Song> filterDuplicates(List<Song> songs);
    
    /**
     * Busca canciones similares en una lista.
     * 
     * @param songs Lista de canciones donde buscar
     * @param threshold Umbral de similitud (0.0 - 1.0)
     * @return Lista de grupos de canciones similares
     */
    List<List<Song>> findSimilarSongs(List<Song> songs, double threshold);
    
    /**
     * Carga la lista de canciones descargadas desde el almacenamiento.
     * 
     * @return Lista de canciones descargadas
     */
    List<Song> loadDownloadedSongs();
    
    /**
     * Guarda la lista de canciones descargadas en el almacenamiento.
     * 
     * @param songs Lista de canciones a guardar
     */
    void saveDownloadedSongs(List<Song> songs);
    
    /**
     * Actualiza el cache de canciones descargadas.
     * 
     * @param songs Lista actualizada de canciones
     */
    void updateCache(List<Song> songs);
    
    /**
     * Verifica si una canción ya existe en el cache.
     * 
     * @param song Canción a verificar
     * @return true si la canción existe, false en caso contrario
     */
    boolean songExists(Song song);
    
    /**
     * Verifica si una URL es válida para descarga.
     * 
     * @param url URL a verificar
     * @return true si la URL es válida, false en caso contrario
     */
    boolean isValidUrl(String url);
}