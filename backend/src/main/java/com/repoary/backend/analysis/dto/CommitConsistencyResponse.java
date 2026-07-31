package com.repoary.backend.analysis.dto;

import java.time.LocalDate;
import java.util.List;

public record CommitConsistencyResponse(
        LocalDate from,
        LocalDate to,
        int commitCount,
        int consistentCount,
        int inconsistentCount,
        List<ConsistencyGroupResponse> groups
) {
}