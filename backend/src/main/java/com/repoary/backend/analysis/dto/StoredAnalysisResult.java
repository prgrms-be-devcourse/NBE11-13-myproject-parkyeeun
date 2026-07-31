package com.repoary.backend.analysis.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StoredAnalysisResult(
        LocalDate targetDate,
        int commitCount,
        List<StoredCommitAnalysis> commits
) {

    public StoredAnalysisResult {
        commits = commits == null
                ? List.of()
                : List.copyOf(commits);
    }

    public record StoredCommitAnalysis(
            String sha,
            String message,
            Instant committedAt,
            String commitType,
            String scope,
            List<String> categories,
            List<StoredFileAnalysis> files
    ) {

        public StoredCommitAnalysis {
            categories = categories == null
                    ? List.of()
                    : List.copyOf(categories);

            files = files == null
                    ? List.of()
                    : List.copyOf(files);
        }
    }

    public record StoredFileAnalysis(
            String filename,
            String status,
            String previousFilename,
            String category,
            String scope
    ) {
    }
}