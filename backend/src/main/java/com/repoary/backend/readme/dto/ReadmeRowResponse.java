package com.repoary.backend.readme.dto;

import java.time.LocalDate;

public record ReadmeRowResponse(
        LocalDate targetDate,
        int weekNumber,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String weekLabel,
        String summary,
        String markdown
) {
}