package com.repoary.backend.analysis.dto;

import java.time.Instant;
import java.util.List;

public record ConsistencyCommitResponse(
        String sha,
        String message,
        String htmlUrl,
        Instant committedAt,
        String commitType,
        String scope,
        String category,
        boolean consistent,
        List<String> issues
) {
}