package com.repoary.backend.recordcheck.dto;

import java.time.YearMonth;
import java.util.List;

public record RecordCheckResponse(
        YearMonth month,
        List<RecordCheckItemResponse> items
) {
}