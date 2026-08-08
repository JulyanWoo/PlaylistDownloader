package com.example.interfaz.model.analyzer;

import java.util.ArrayList;
import java.util.List;

public class LibraryAnalysisResult {

    private final long totalSongs;
    private final long totalFiles;
    private final long totalDuplicates;
    private final long recoverableSpaceBytes;
    private final long totalDurationMillis;
    private final List<DuplicateGroup> groups;
    /** All songs scanned, with language already detected (used by the Language Browser). */
    private final List<SongFile> allSongs;

    public LibraryAnalysisResult(long totalSongs, long totalFiles, long totalDuplicates,
                                 long recoverableSpaceBytes, long totalDurationMillis,
                                 List<DuplicateGroup> groups) {
        this(totalSongs, totalFiles, totalDuplicates, recoverableSpaceBytes, totalDurationMillis,
                groups, new ArrayList<>());
    }

    public LibraryAnalysisResult(long totalSongs, long totalFiles, long totalDuplicates,
                                 long recoverableSpaceBytes, long totalDurationMillis,
                                 List<DuplicateGroup> groups, List<SongFile> allSongs) {
        this.totalSongs = totalSongs;
        this.totalFiles = totalFiles;
        this.totalDuplicates = totalDuplicates;
        this.recoverableSpaceBytes = recoverableSpaceBytes;
        this.totalDurationMillis = totalDurationMillis;
        this.groups = groups != null ? groups : new ArrayList<>();
        this.allSongs = allSongs != null ? allSongs : new ArrayList<>();
    }

    public long getTotalSongs() {
        return totalSongs;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public long getTotalDuplicates() {
        return totalDuplicates;
    }

    public long getRecoverableSpaceBytes() {
        return recoverableSpaceBytes;
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public List<DuplicateGroup> getGroups() {
        return groups;
    }

    /** Returns all scanned songs with language information populated. */
    public List<SongFile> getAllSongs() {
        return allSongs;
    }
}
