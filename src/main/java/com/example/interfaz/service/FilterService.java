package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import java.util.List;

public interface FilterService {

    List<Song> filterDuplicates(List<Song> songs);

    List<List<Song>> findSimilarSongs(List<Song> songs, double threshold);

    List<Song> loadDownloadedSongs();

    void saveDownloadedSongs(List<Song> songs);

    void updateCache(List<Song> songs);

    boolean songExists(Song song);

    boolean isValidUrl(String url);
}
