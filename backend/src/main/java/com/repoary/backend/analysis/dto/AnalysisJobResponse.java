package com.repoary.backend.analysis.dto;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Schema(description = "커밋 분석 작업 응답")
public record AnalysisJobResponse(
        Long id,
        Long connectedRepositoryId,
        LocalDate targetDate,
        AnalysisJobStatus status,
        JsonNode result,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static AnalysisJobResponse from(
            AnalysisJob analysisJob,
            JsonMapper jsonMapper
    ) {
        JsonNode result = analysisJob.getResult() == null
                ? null
                : jsonMapper.readTree(analysisJob.getResult());

        return new AnalysisJobResponse(
                analysisJob.getId(),
                analysisJob.getConnectedRepository().getId(),
                analysisJob.getTargetDate(),
                analysisJob.getStatus(),
                result,
                analysisJob.getErrorMessage(),
                toUtcOffsetDateTime(analysisJob.getStartedAt()),
                toUtcOffsetDateTime(analysisJob.getCompletedAt()),
                toUtcOffsetDateTime(analysisJob.getCreatedAt()),
                toUtcOffsetDateTime(analysisJob.getUpdatedAt())
        );
    }

    private static OffsetDateTime toUtcOffsetDateTime(
            LocalDateTime dateTime
    ) {
        return dateTime == null
                ? null
                : dateTime.atOffset(ZoneOffset.UTC);
    }
}