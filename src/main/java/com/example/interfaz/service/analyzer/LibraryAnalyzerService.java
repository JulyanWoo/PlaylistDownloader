package com.example.interfaz.service.analyzer;

import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.*;
import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.LibraryAnalysisResult;
import com.example.interfaz.model.analyzer.SongFile;
import com.example.interfaz.service.config.MusicFolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class LibraryAnalyzerService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryAnalyzerService.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "flac", "wav", "m4a", "ogg"
    ));

    private final MusicFolderService musicFolderService;
    private final SongMetadataReader metadataReader;
    private final DuplicateDetectionService duplicateDetectionService;
    private final EventPublisher eventPublisher;

    private final ExecutorService executorService;
    private Future<?> currentTask;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public LibraryAnalyzerService(MusicFolderService musicFolderService,
                                  SongMetadataReader metadataReader,
                                  DuplicateDetectionService duplicateDetectionService,
                                  EventPublisher eventPublisher) {
        this.musicFolderService = musicFolderService;
        this.metadataReader = metadataReader != null ? metadataReader : new SongMetadataReader();
        this.duplicateDetectionService = duplicateDetectionService != null ? duplicateDetectionService : new DuplicateDetectionService();
        this.eventPublisher = eventPublisher;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LibraryAnalyzerWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void startAnalysis() {
        if (currentTask != null && !currentTask.isDone()) {
            LOGGER.warn("Library analysis is already running.");
            return;
        }

        cancelled.set(false);
        String folderPath = musicFolderService.getCurrentMusicFolder();

        if (eventPublisher != null) {
            eventPublisher.publish(new LibraryAnalysisStarted(folderPath));
        }

        currentTask = executorService.submit(() -> runAnalysis(folderPath));
    }

    public synchronized void cancelAnalysis() {
        cancelled.set(true);
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    public boolean isAnalyzing() {
        return currentTask != null && !currentTask.isDone();
    }

    private void runAnalysis(String folderPath) {
        long startTime = System.currentTimeMillis();
        Path musicPath = Paths.get(folderPath);

        if (!Files.exists(musicPath) || !Files.isDirectory(musicPath)) {
            LOGGER.error("Music folder does not exist: {}", folderPath);
            LibraryAnalysisResult emptyResult = new LibraryAnalysisResult(0, 0, 0, 0, 0, new ArrayList<>());
            if (eventPublisher != null) {
                eventPublisher.publish(new LibraryAnalysisFinished(emptyResult));
            }
            return;
        }

        List<Path> audioFiles = new ArrayList<>();
        try (var stream = Files.walk(musicPath)) {
            audioFiles = stream.filter(this::isAudioFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error traversing music directory: {}", folderPath, e);
        }

        long totalFiles = audioFiles.size();
        List<SongFile> processedSongs = new ArrayList<>();
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        long processedCount = 0;

        for (Path filePath : audioFiles) {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                LOGGER.info("Library analysis cancelled during scanning.");
                publishCancelledResult(processedCount, totalFiles, duplicateGroups, startTime);
                return;
            }

            SongFile songFile = metadataReader.readMetadata(filePath);
            if (songFile != null) {
                processedSongs.add(songFile);
            }
            processedCount++;

            long elapsed = System.currentTimeMillis() - startTime;
            long estimatedRemaining = (processedCount > 0)
                    ? (elapsed * (totalFiles - processedCount)) / processedCount
                    : 0;

            if (processedCount % 5 == 0 || processedCount == totalFiles) {
                if (eventPublisher != null) {
                    eventPublisher.publish(new LibraryProgressUpdated(
                            processedCount,
                            totalFiles,
                            countDuplicates(duplicateGroups),
                            "Leyendo metadatos: " + filePath.getFileName(),
                            elapsed,
                            estimatedRemaining
                    ));
                }
            }
        }

        // Run duplicate detection algorithm
        if (!cancelled.get()) {
            duplicateGroups = duplicateDetectionService.detectDuplicates(processedSongs);
        }

        if (cancelled.get()) {
            publishCancelledResult(processedCount, totalFiles, duplicateGroups, startTime);
            return;
        }

        long totalDuplicates = countDuplicates(duplicateGroups);
        long recoverableSpace = calculateRecoverableSpace(duplicateGroups);
        long totalDurationMillis = System.currentTimeMillis() - startTime;

        LibraryAnalysisResult finalResult = new LibraryAnalysisResult(
                processedSongs.size(),
                totalFiles,
                totalDuplicates,
                recoverableSpace,
                totalDurationMillis,
                duplicateGroups
        );

        if (eventPublisher != null) {
            eventPublisher.publish(new LibraryAnalysisFinished(finalResult));
        }

        LOGGER.info("Library analysis finished. Songs: {}, Duplicates: {}", processedSongs.size(), totalDuplicates);
    }

    private void publishCancelledResult(long processedCount, long totalFiles, List<DuplicateGroup> groups, long startTime) {
        long totalDuplicates = countDuplicates(groups);
        long recoverableSpace = calculateRecoverableSpace(groups);
        long elapsed = System.currentTimeMillis() - startTime;

        LibraryAnalysisResult partialResult = new LibraryAnalysisResult(
                processedCount,
                totalFiles,
                totalDuplicates,
                recoverableSpace,
                elapsed,
                groups
        );

        if (eventPublisher != null) {
            eventPublisher.publish(new LibraryAnalysisCancelled(partialResult));
        }
    }

    public boolean isAudioFile(Path path) {
        if (path == null || Files.isDirectory(path)) return false;
        try {
            if (Files.isHidden(path)) return false;
        } catch (IOException ignored) {}

        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".part") || name.endsWith(".temp") || name.endsWith(".tmp")) {
            return false;
        }

        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < name.length() - 1) {
            String ext = name.substring(dotIdx + 1);
            return SUPPORTED_EXTENSIONS.contains(ext);
        }
        return false;
    }

    public int moveToQuarantine(List<DuplicateCandidate> selectedCandidates) {
        if (selectedCandidates == null || selectedCandidates.isEmpty()) {
            return 0;
        }

        String baseFolder = musicFolderService.getCurrentMusicFolder();
        Path quarantineDir = Paths.get(baseFolder, ".duplicates");

        try {
            if (!Files.exists(quarantineDir)) {
                Files.createDirectories(quarantineDir);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create quarantine directory: {}", quarantineDir, e);
            return 0;
        }

        int movedCount = 0;
        for (DuplicateCandidate candidate : selectedCandidates) {
            if (candidate.isSelectedForDeletion() && !candidate.isOriginal()) {
                Path source = candidate.getSongFile().getPath();
                Path target = quarantineDir.resolve(source.getFileName());
                try {
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                    movedCount++;
                    LOGGER.info("Moved duplicate to quarantine: {} -> {}", source, target);
                } catch (IOException e) {
                    LOGGER.error("Failed to move file to quarantine: {}", source, e);
                }
            }
        }

        return movedCount;
    }

    private long countDuplicates(List<DuplicateGroup> groups) {
        long count = 0;
        if (groups != null) {
            for (DuplicateGroup group : groups) {
                if (group.getCandidates() != null && group.getCandidates().size() > 1) {
                    count += (group.getCandidates().size() - 1);
                }
            }
        }
        return count;
    }

    private long calculateRecoverableSpace(List<DuplicateGroup> groups) {
        long bytes = 0;
        if (groups != null) {
            for (DuplicateGroup group : groups) {
                if (group.getCandidates() != null) {
                    for (DuplicateCandidate candidate : group.getCandidates()) {
                        if (candidate.isSelectedForDeletion()) {
                            bytes += candidate.getSongFile().getSize();
                        }
                    }
                }
            }
        }
        return bytes;
    }

    @Override
    public void close() {
        cancelAnalysis();
        executorService.shutdownNow();
    }
}
