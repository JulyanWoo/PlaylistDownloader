package com.example.interfaz.service.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.model.analyzer.SongFile;

public class DuplicateDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateDetectionService.class);
    private static final int DURATION_TOLERANCE_SECONDS = 2;

    public List<DuplicateGroup> detectDuplicates(List<SongFile> songs) {
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        if (songs == null || songs.isEmpty()) {
            return duplicateGroups;
        }

        // Level 1: Group by normalizedName
        Map<String, List<SongFile>> level1Groups = new HashMap<>();
        for (SongFile song : songs) {
            String normName = song.getNormalizedName();
            if (normName != null && !normName.isEmpty()) {
                level1Groups.computeIfAbsent(normName, k -> new ArrayList<>()).add(song);
            }
        }

        for (Map.Entry<String, List<SongFile>> entry : level1Groups.entrySet()) {
            List<SongFile> groupSongs = entry.getValue();
            if (groupSongs.size() < 2) {
                continue;
            }

            // Level 2: Sub-group by duration (tolerance ±2 seconds)
            List<List<SongFile>> level2SubGroups = subGroupByDuration(groupSongs);

            for (List<SongFile> subGroup : level2SubGroups) {
                if (subGroup.size() < 2) {
                    continue;
                }

                // Level 3: Metadata check (Title, Artist, Album)
                boolean metadataMatches = checkMetadataMatch(subGroup);

                // Level 4: SHA-256 confirmation on demand
                boolean hashesMatch = checkAndComputeHashes(subGroup);

                // Classification
                GroupClassification classification;
                if (hashesMatch) {
                    classification = GroupClassification.CONFIRMED_DUPLICATE;
                } else if (metadataMatches) {
                    classification = GroupClassification.CONFIRMED_DUPLICATE;
                } else {
                    classification = GroupClassification.POSSIBLE_DUPLICATE;
                }

                List<DuplicateCandidate> candidates = createCandidates(subGroup);
                String groupTitle = subGroup.get(0).getTitle() != null && !subGroup.get(0).getTitle().isEmpty()
                        ? subGroup.get(0).getTitle()
                        : entry.getKey();

                duplicateGroups.add(new DuplicateGroup(groupTitle, candidates, classification));
            }
        }

        return duplicateGroups;
    }

    private List<List<SongFile>> subGroupByDuration(List<SongFile> songs) {
        List<List<SongFile>> subGroups = new ArrayList<>();
        List<SongFile> pool = new ArrayList<>(songs);

        while (!pool.isEmpty()) {
            SongFile base = pool.remove(0);
            List<SongFile> cluster = new ArrayList<>();
            cluster.add(base);

            List<SongFile> remaining = new ArrayList<>();
            for (SongFile s : pool) {
                if (Math.abs(base.getDuration() - s.getDuration()) <= DURATION_TOLERANCE_SECONDS) {
                    cluster.add(s);
                } else {
                    remaining.add(s);
                }
            }
            subGroups.add(cluster);
            pool = remaining;
        }

        return subGroups;
    }

    private boolean checkMetadataMatch(List<SongFile> songs) {
        if (songs.size() < 2) return true;
        SongFile first = songs.get(0);
        for (int i = 1; i < songs.size(); i++) {
            SongFile other = songs.get(i);
            if (first.getArtist() != null && !first.getArtist().isEmpty()
                    && other.getArtist() != null && !other.getArtist().isEmpty()) {
                if (first.getArtist().equalsIgnoreCase(other.getArtist())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAndComputeHashes(List<SongFile> songs) {
        if (songs.size() < 2) return false;

        for (SongFile song : songs) {
            if (song.getHash() == null) {
                song.setHash(computeSha256(song.getPath()));
            }
        }

        String firstHash = songs.get(0).getHash();
        if (firstHash == null || firstHash.isEmpty()) {
            return false;
        }

        for (int i = 1; i < songs.size(); i++) {
            if (!firstHash.equals(songs.get(i).getHash())) {
                return false;
            }
        }
        return true;
    }

    public String computeSha256(Path path) {
        if (path == null || !Files.exists(path)) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // Read file
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("Failed to compute SHA-256 for path: {}", path, e);
            return "";
        }
    }

    private List<DuplicateCandidate> createCandidates(List<SongFile> songs) {
        List<DuplicateCandidate> candidates = new ArrayList<>();

        // Pick original based on file size/quality (largest file size marked as original by default)
        SongFile bestFile = songs.get(0);
        for (SongFile s : songs) {
            if (s.getSize() > bestFile.getSize()) {
                bestFile = s;
            }
        }

        for (SongFile s : songs) {
            boolean isOriginal = (s == bestFile);
            DuplicateCandidate candidate = new DuplicateCandidate(s, !isOriginal, isOriginal);
            candidates.add(candidate);
        }

        return candidates;
    }
}
