package com.repoary.backend.recordcheck.dto;

import java.time.LocalDate;

public record RecordCheckItemResponse(
        LocalDate date,
        boolean tilExists,
        boolean readmeEntryExists
) {
}