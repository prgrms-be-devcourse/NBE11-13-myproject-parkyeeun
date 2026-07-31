package com.repoary.backend.analysis.dto;

import java.time.Instant;
import java.util.List;

public record CommitAnalysisResponse(
        String sha,
        String message,
        String htmlUrl,
        Instant committedAt,
        ConventionMatchResult convention,
        List<FileAnalysis> files
) {

    public CommitAnalysisResponse {
        files = files == null ? List.of() : List.copyOf(files);
    }

    public record FileAnalysis(
            String filename,
            String status,
            int additions,
            int deletions,
            int changes,
            String previousFilename,
            ClassificationMatchResult classification
    ) {
    }
}