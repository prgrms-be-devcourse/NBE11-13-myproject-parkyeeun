package com.repoary.backend.analysis.dto;

import java.time.LocalDate;

public record CommitConsistencyRequest(
        LocalDate from,
        LocalDate to
) {
}