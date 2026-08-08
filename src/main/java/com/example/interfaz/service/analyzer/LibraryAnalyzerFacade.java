package com.example.interfaz.service.analyzer;

import java.util.List;

import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisCancelled;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisFinished;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryAnalysisStarted;
import com.example.interfaz.event.analyzer.LibraryAnalyzerEvent.LibraryProgressUpdated;
import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.LanguageDetectorMode;
import com.example.interfaz.model.analyzer.SongFile;

public class LibraryAnalyzerFacade implements AutoCloseable {

    private final LibraryAnalyzerService libraryAnalyzerService;
    private final DuplicateSelectionService duplicateSelectionService;
    private final DuplicateManagementService duplicateManagementService;
    private final SongLanguageBrowserService songLanguageBrowserService;
    private final EventPublisher eventPublisher;

    public interface AnalysisStartedHandler { void onStarted(LibraryAnalysisStarted event); }
    public interface ProgressUpdatedHandler { void onProgress(LibraryProgressUpdated event); }
    public interface AnalysisFinishedHandler { void onFinished(LibraryAnalysisFinished event); }
    public interface AnalysisCancelledHandler { void onCancelled(LibraryAnalysisCancelled event); }

    private AnalysisStartedHandler startedHandler;
    private ProgressUpdatedHandler progressHandler;
    private AnalysisFinishedHandler finishedHandler;
    private AnalysisCancelledHandler cancelledHandler;

    public LibraryAnalyzerFacade(
            LibraryAnalyzerService libraryAnalyzerService,
            DuplicateSelectionService duplicateSelectionService,
            DuplicateManagementService duplicateManagementService,
            SongLanguageBrowserService songLanguageBrowserService,
            EventPublisher eventPublisher
    ) {
        this.libraryAnalyzerService = libraryAnalyzerService;
        this.duplicateSelectionService = duplicateSelectionService;
        this.duplicateManagementService = duplicateManagementService;
        this.songLanguageBrowserService = songLanguageBrowserService;
        this.eventPublisher = eventPublisher;
    }

    public void startAnalysis(LanguageDetectorMode mode) {
        if (libraryAnalyzerService != null) {
            libraryAnalyzerService.setLanguageDetectorMode(mode);
            libraryAnalyzerService.startAnalysis();
        }
    }

    public void cancelAnalysis() {
        if (libraryAnalyzerService != null) {
            libraryAnalyzerService.cancelAnalysis();
        }
    }

    public boolean isAnalyzing() {
        return libraryAnalyzerService != null && libraryAnalyzerService.isAnalyzing();
    }

    public DuplicateSelectionService.SelectionSummary calculateSelectionSummary(List<DuplicateGroup> groups) {
        return duplicateSelectionService.calculateSelectionSummary(groups);
    }

    public void keepOriginals(List<DuplicateGroup> groups) {
        duplicateSelectionService.keepOriginals(groups);
    }

    public void selectDuplicates(List<DuplicateGroup> groups) {
        duplicateSelectionService.selectDuplicates(groups);
    }

    public void unselectAll(List<DuplicateGroup> groups) {
        duplicateSelectionService.unselectAll(groups);
    }

    public List<DuplicateCandidate> getSelectedCandidates(List<DuplicateGroup> groups) {
        return duplicateSelectionService.getSelectedCandidates(groups);
    }

    public DuplicateManagementService.QuarantineResult moveSelectedToQuarantine(
            List<DuplicateCandidate> selectedCandidates, List<DuplicateGroup> currentGroups) {
        return duplicateManagementService.moveSelectedToQuarantine(selectedCandidates, currentGroups);
    }

    public SongLanguageBrowserService.QuarantineResult moveSelectedSongsToQuarantine(List<SongFile> songs) {
        return songLanguageBrowserService.moveToQuarantine(songs);
    }

    public void subscribeToEvents(
            AnalysisStartedHandler onStarted,
            ProgressUpdatedHandler onProgress,
            AnalysisFinishedHandler onFinished,
            AnalysisCancelledHandler onCancelled
    ) {
        this.startedHandler = onStarted;
        this.progressHandler = onProgress;
        this.finishedHandler = onFinished;
        this.cancelledHandler = onCancelled;

        if (eventPublisher != null) {
            eventPublisher.subscribe(LibraryAnalysisStarted.class, this::handleStarted);
            eventPublisher.subscribe(LibraryProgressUpdated.class, this::handleProgress);
            eventPublisher.subscribe(LibraryAnalysisFinished.class, this::handleFinished);
            eventPublisher.subscribe(LibraryAnalysisCancelled.class, this::handleCancelled);
        }
    }

    public void unsubscribeFromEvents() {
        if (eventPublisher != null) {
            eventPublisher.unsubscribe(LibraryAnalysisStarted.class, this::handleStarted);
            eventPublisher.unsubscribe(LibraryProgressUpdated.class, this::handleProgress);
            eventPublisher.unsubscribe(LibraryAnalysisFinished.class, this::handleFinished);
            eventPublisher.unsubscribe(LibraryAnalysisCancelled.class, this::handleCancelled);
        }
    }

    private void handleStarted(LibraryAnalysisStarted e) { if (startedHandler != null) startedHandler.onStarted(e); }
    private void handleProgress(LibraryProgressUpdated e) { if (progressHandler != null) progressHandler.onProgress(e); }
    private void handleFinished(LibraryAnalysisFinished e) { if (finishedHandler != null) finishedHandler.onFinished(e); }
    private void handleCancelled(LibraryAnalysisCancelled e) { if (cancelledHandler != null) cancelledHandler.onCancelled(e); }

    @Override
    public void close() {
        unsubscribeFromEvents();
    }
}
