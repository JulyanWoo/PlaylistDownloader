package com.example.interfaz.service;

import java.util.List;

import com.example.interfaz.model.Song;

/**
 * Contract for URL and song validation/filtering within the current session
 * queue. No persistent file-based history is used.
 */
public interface FilterService {

    /**
     * Returns true if any song in the given list has a duplicate title.
     */
    List<Song> filterDuplicates(List<Song> songs);

    /**
     * Groups songs that appear similar based on a similarity threshold.
     */
    List<List<Song>> findSimilarSongs(List<Song> songs, double threshold);

    /**
     * Returns true if a URL is valid and well-formed.
     */
    boolean isValidUrl(String url);
}
