package com.example.interfaz.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.Song;
import com.example.interfaz.service.filter.DuplicateFinder;
import com.example.interfaz.service.filter.SimilarityCalculator;
import com.example.interfaz.service.filter.TitleNormalizer;

/**
 * Provides in-session duplicate filtering and URL validation.
 * No persistent file history (canciones.txt) is used.
 */
public class SongFilterService implements FilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SongFilterService.class);
    private static final double SIMILARITY_THRESHOLD = 0.70;

    private final DuplicateFinder duplicateFinder;

    public SongFilterService() {
        this.duplicateFinder = new DuplicateFinder(SIMILARITY_THRESHOLD);
        LOGGER.info("SongFilterService initialized (no persistent song history)");
    }

    /** Checks for duplicate among a given set of known titles (session-only). */
    public boolean isDuplicateSong(String songTitle, Set<String> knownTitles) {
        if (songTitle == null || songTitle.trim().isEmpty() || knownTitles == null) {
            return false;
        }
        return duplicateFinder.isDuplicate(songTitle, knownTitles);
    }

    public double calculateSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null) return 0.0;
        String n1 = TitleNormalizer.normalize(title1);
        String n2 = TitleNormalizer.normalize(title2);
        return SimilarityCalculator.calculateLevenshteinSimilarity(n1, n2);
    }

    public boolean areSimilar(String title1, String title2) {
        return SimilarityCalculator.areSimilar(title1, title2, SIMILARITY_THRESHOLD);
    }

    @Override
    public List<Song> filterDuplicates(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return new ArrayList<>();

        List<Song> result = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();

        for (Song song : songs) {
            if (song != null && song.getTitle() != null) {
                String normalized = TitleNormalizer.normalize(song.getTitle());
                if (seenTitles.add(normalized)) {
                    result.add(song);
                }
            }
        }
        return result;
    }

    @Override
    public List<List<Song>> findSimilarSongs(List<Song> songs, double threshold) {
        List<List<Song>> groups = new ArrayList<>();
        List<Song> processed = new ArrayList<>();

        for (Song song : songs) {
            if (processed.contains(song)) continue;
            List<Song> group = new ArrayList<>();
            group.add(song);
            processed.add(song);

            for (Song other : songs) {
                if (!processed.contains(other)
                        && calculateSimilarity(song.getTitle(), other.getTitle()) >= threshold) {
                    group.add(other);
                    processed.add(other);
                }
            }
            if (group.size() > 1) groups.add(group);
        }
        return groups;
    }

    @Override
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        try {
            new URL(url.trim());
            return url.contains("youtube.com") || url.contains("youtu.be");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
