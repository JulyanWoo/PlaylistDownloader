package com.example.interfaz.service.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.config.MusicFolderService;

/**
 * Service for managing song quarantine from the Language Browser.
 * Moves selected songs to a ".quarantine-idioma" subfolder inside the music library.
 */
public class SongLanguageBrowserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SongLanguageBrowserService.class);
    private static final String QUARANTINE_FOLDER = ".quarantine-idioma";

    private final MusicFolderService musicFolderService;

    public SongLanguageBrowserService(MusicFolderService musicFolderService) {
        this.musicFolderService = musicFolderService;
    }

    public record QuarantineResult(int moved, int failed, List<String> failedPaths) {}

    /**
     * Moves the given list of songs to the language quarantine folder.
     *
     * @param songs list of SongFile objects to quarantine
     * @return result summary
     */
    public QuarantineResult moveToQuarantine(List<SongFile> songs) {
        if (songs == null || songs.isEmpty() || musicFolderService == null) {
            return new QuarantineResult(0, 0, List.of());
        }

        String baseFolder = musicFolderService.getCurrentMusicFolder();
        if (baseFolder == null || baseFolder.isBlank()) {
            LOGGER.error("Music folder not configured, cannot quarantine files.");
            return new QuarantineResult(0, songs.size(), songs.stream()
                    .map(s -> s.getPath() != null ? s.getPath().toString() : "?")
                    .toList());
        }

        Path quarantineDir = Paths.get(baseFolder, QUARANTINE_FOLDER);
        try {
            if (!Files.exists(quarantineDir)) {
                Files.createDirectories(quarantineDir);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create language quarantine directory: {}", quarantineDir, e);
            return new QuarantineResult(0, songs.size(), List.of());
        }

        int moved = 0;
        int failed = 0;
        List<String> failedPaths = new java.util.ArrayList<>();

        for (SongFile song : songs) {
            Path source = song.getPath();
            if (source == null || !Files.exists(source)) {
                LOGGER.warn("File not found for quarantine: {}", source);
                failedPaths.add(source != null ? source.toString() : "?");
                failed++;
                continue;
            }

            Path target = quarantineDir.resolve(source.getFileName());
            // Avoid name collision
            if (Files.exists(target)) {
                String baseName = source.getFileName().toString();
                int dot = baseName.lastIndexOf('.');
                String nameOnly = dot > 0 ? baseName.substring(0, dot) : baseName;
                String ext = dot > 0 ? baseName.substring(dot) : "";
                target = quarantineDir.resolve(nameOnly + "_" + System.currentTimeMillis() + ext);
            }

            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                moved++;
                LOGGER.info("Moved to language quarantine: {} -> {}", source, target);
            } catch (IOException e) {
                LOGGER.error("Failed to move file: {}", source, e);
                failedPaths.add(source.toString());
                failed++;
            }
        }

        return new QuarantineResult(moved, failed, failedPaths);
    }
}
