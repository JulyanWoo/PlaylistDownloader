package com.example.interfaz.service.analyzer;

import java.util.ArrayList;
import java.util.List;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;

public class DuplicateManagementService {

    public record QuarantineResult(int countMoved, int countFailed, List<String> failedFilePaths, List<DuplicateGroup> remainingGroups) {

        public QuarantineResult(int countMoved, List<DuplicateGroup> remainingGroups) {
            this(countMoved, 0, List.of(), remainingGroups);
        }
    }

    private final LibraryAnalyzerService analyzerService;

    public DuplicateManagementService(LibraryAnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    public DuplicateManagementService() {
        this(null);
    }

    public QuarantineResult moveSelectedToQuarantine(List<DuplicateCandidate> selectedCandidates,
            List<DuplicateGroup> currentGroups) {
        if (selectedCandidates == null || selectedCandidates.isEmpty() || analyzerService == null) {
            return new QuarantineResult(0, 0, List.of(), currentGroups != null ? currentGroups : List.of());
        }

        int moved = analyzerService.moveToQuarantine(selectedCandidates);
        int failed = selectedCandidates.size() - moved;
        List<String> failedPaths = new ArrayList<>();

        List<DuplicateGroup> remainingGroups = new ArrayList<>();
        if (currentGroups != null) {
            for (DuplicateGroup group : currentGroups) {
                List<DuplicateCandidate> remainingCandidates = new ArrayList<>();
                for (DuplicateCandidate candidate : group.getCandidates()) {
                    if (!selectedCandidates.contains(candidate)) {
                        remainingCandidates.add(candidate);
                    }
                }
                if (remainingCandidates.size() > 1) {
                    remainingGroups.add(new DuplicateGroup(group.getGroupName(), remainingCandidates, group.getClassification(), group.getConfidence()));
                }
            }
        }

        return new QuarantineResult(moved, failed, failedPaths, remainingGroups);
    }
}
