package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import com.example.interfaz.util.FileUtils;
import com.example.interfaz.service.filter.DuplicateFinder;
import com.example.interfaz.service.filter.SimilarityCalculator;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SongFilterService implements FilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SongFilterService.class);

    private static final double SIMILARITY_THRESHOLD = 0.70;

    private Set<String> downloadedSongs;
    private long lastCacheUpdate;
    private static final long CACHE_EXPIRY_MS = 30000;

    private static SongFilterService instance;

    private final DuplicateFinder duplicateFinder;

    private SongFilterService() {
        this.downloadedSongs = new HashSet<>();
        this.lastCacheUpdate = 0;
        this.duplicateFinder = new DuplicateFinder(SIMILARITY_THRESHOLD);
        loadDownloadedSongs();
    }

    public static synchronized SongFilterService getInstance() {
        if (instance == null) {
            instance = new SongFilterService();
        }
        return instance;
    }

    public boolean isDuplicateSong(String songTitle) {
        if (songTitle == null || songTitle.trim().isEmpty()) {
            return false;
        }

        refreshCacheIfNeeded();

        return duplicateFinder.isDuplicate(songTitle, downloadedSongs);
    }

    public boolean isDuplicateSong(Song song) {
        if (song == null || song.getTitle() == null) {
            return false;
        }

        return isDuplicateSong(song.getTitle());
    }

    public Map<String, List<String>> groupSimilarSongs(List<String> songTitles) {
        return duplicateFinder.groupSimilarTitles(songTitles);
    }

    public void registerDownloadedSong(String songTitle) {
        if (songTitle != null && !songTitle.trim().isEmpty()) {
            downloadedSongs.add(songTitle.trim());
            FileUtils.saveDownloadedSong(songTitle.trim());
            LOGGER.info("Canción registrada: " + songTitle);
        }
    }

    public void registerDownloadedSong(Song song) {
        if (song != null && song.getTitle() != null) {
            registerDownloadedSong(song.getTitle());
            song.setDownloaded(true);
        }
    }

    public Set<String> getDownloadedSongs() {
        refreshCacheIfNeeded();
        return new HashSet<>(downloadedSongs);
    }

    public double calculateSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null) {
            return 0.0;
        }

        String normalized1 = normalizeTitle(title1);
        String normalized2 = normalizeTitle(title2);

        int maxLength = Math.max(normalized1.length(), normalized2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(normalized1, normalized2);
        return 1.0 - (double) distance / maxLength;
    }

    public boolean areSimilar(String title1, String title2) {
        return SimilarityCalculator.areSimilar(title1, title2, SIMILARITY_THRESHOLD);
    }

    public List<Song> loadDownloadedSongs() {
        try {
            this.downloadedSongs = FileUtils.loadDownloadedSongs();
            this.lastCacheUpdate = System.currentTimeMillis();
            LOGGER.info("Cache de canciones actualizado: {} canciones", downloadedSongs.size());
        } catch (Exception e) {
            LOGGER.error("Error al cargar canciones descargadas", e);
            this.downloadedSongs = new HashSet<>();
        }
        return new ArrayList<>();
    }

    @Override
    public void updateCache(List<Song> songs) {
        if (songs != null) {
            this.downloadedSongs = new HashSet<>();
            for (Song song : songs) {
                if (song != null && song.getTitle() != null) {
                    this.downloadedSongs.add(song.getTitle());
                }
            }
            LOGGER.info("Cache actualizado con " + songs.size() + " canciones");
        }
    }

    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_EXPIRY_MS) {
            loadDownloadedSongs();
        }
    }

    public void clearCache() {
        this.lastCacheUpdate = 0;
        loadDownloadedSongs();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDownloadedSongs", downloadedSongs.size());
        stats.put("similarityThreshold", SIMILARITY_THRESHOLD);
        stats.put("lastCacheUpdate", new Date(lastCacheUpdate));
        return stats;
    }

    public boolean songExists(Song song) {
        if (song == null || song.getTitle() == null) {
            return false;
        }

        return isDuplicateSong(song.getTitle());
    }

    @Override
    public List<Song> filterDuplicates(List<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Song> filteredSongs = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();

        for (Song song : songs) {
            if (song != null && song.getTitle() != null) {
                String normalizedTitle = normalizeTitle(song.getTitle());
                if (!seenTitles.contains(normalizedTitle)) {
                    seenTitles.add(normalizedTitle);
                    filteredSongs.add(song);
                }
            }
        }

        return filteredSongs;
    }

    @Override
    public List<List<Song>> findSimilarSongs(List<Song> songs, double threshold) {
        List<List<Song>> similarGroups = new ArrayList<>();
        List<Song> processed = new ArrayList<>();

        for (Song song : songs) {
            if (processed.contains(song)) {
                continue;
            }

            List<Song> similarGroup = new ArrayList<>();
            similarGroup.add(song);
            processed.add(song);

            for (Song otherSong : songs) {
                if (!processed.contains(otherSong) && 
                    calculateSimilarity(song.getTitle(), otherSong.getTitle()) >= threshold) {
                    similarGroup.add(otherSong);
                    processed.add(otherSong);
                }
            }

            if (similarGroup.size() > 1) {
                similarGroups.add(similarGroup);
            }
        }

        return similarGroups;
    }

    @Override
    public void saveDownloadedSongs(List<Song> songs) {
        if (songs != null) {
            downloadedSongs.clear();
            for (Song song : songs) {
                if (song != null && song.getTitle() != null) {
                    downloadedSongs.add(song.getTitle());
                }
            }
            LOGGER.info("Guardadas " + songs.size() + " canciones en la caché");
         }
     }

      private String normalizeTitle(String title) {
          if (title == null) {
              return "";
          }

          return title.toLowerCase()
                      .replaceAll("[^a-z0-9\\s]", "")
                      .replaceAll("\\s+", " ")
                      .trim();
      }

     private int levenshteinDistance(String s1, String s2) {
         int[][] dp = new int[s1.length() + 1][s2.length() + 1];

         for (int i = 0; i <= s1.length(); i++) {
             dp[i][0] = i;
         }

         for (int j = 0; j <= s2.length(); j++) {
             dp[0][j] = j;
         }

         for (int i = 1; i <= s1.length(); i++) {
             for (int j = 1; j <= s2.length(); j++) {
                 if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                     dp[i][j] = dp[i - 1][j - 1];
                 } else {
                     dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                 }
             }
         }

         return dp[s1.length()][s2.length()];
     }

     @Override
     public boolean isValidUrl(String url) {
         if (url == null || url.trim().isEmpty()) {
             return false;
         }

         try {
             java.net.URI uri = java.net.URI.create(url);
             java.net.URL urlObj = uri.toURL();
             String protocol = urlObj.getProtocol();
             return "http".equals(protocol) || "https".equals(protocol);
         } catch (Exception e) {
             return false;
         }
     }
 }
