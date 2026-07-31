package com.repoary.backend.analysis.dto;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "분석 작업 목록 응답")
public record AnalysisJobSummaryResponse(

        @Schema(description = "분석 작업 ID", example = "3")
        Long id,

        @Schema(description = "분석 대상 학습 날짜", example = "2026-07-29")
        LocalDate targetDate,

        @Schema(description = "분석 작업 상태", example = "COMPLETED")
        AnalysisJobStatus status,

        @Schema(
                description = "분석 실패 메시지",
                nullable = true
        )
        String errorMessage,

        @Schema(description = "분석 작업 생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "분석 시작 시각")
        LocalDateTime startedAt,

        @Schema(description = "분석 완료 또는 실패 시각")
        LocalDateTime completedAt
) {

    public static AnalysisJobSummaryResponse from(
            AnalysisJob analysisJob
    ) {
        return new AnalysisJobSummaryResponse(
                analysisJob.getId(),
                analysisJob.getTargetDate(),
                analysisJob.getStatus(),
                analysisJob.getErrorMessage(),
                analysisJob.getCreatedAt(),
                analysisJob.getStartedAt(),
                analysisJob.getCompletedAt()
        );
    }
}