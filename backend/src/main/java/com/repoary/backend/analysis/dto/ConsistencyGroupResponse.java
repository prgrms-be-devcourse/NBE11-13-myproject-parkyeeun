package com.repoary.backend.analysis.dto;

import java.util.List;

public record ConsistencyGroupResponse(
        String pathPattern,
        String category,
        String scope,
        String expectedPattern,
        int commitCount,
        int consistentCount,
        int inconsistentCount,
        List<ConsistencyCommitResponse> commits
) {
}