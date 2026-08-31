package com.repoary.backend.til.dto;

import com.repoary.backend.til.domain.TilDocument;
import com.repoary.backend.til.domain.TilDocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Schema(description = "TIL 문서 응답")
public record TilDocumentResponse(
        Long id,
        Long connectedRepositoryId,
        Long analysisJobId,
        LocalDate targetDate,
        String title,
        String content,
        TilDocumentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TilDocumentResponse from(
            TilDocument tilDocument
    ) {
        return new TilDocumentResponse(
                tilDocument.getId(),
                tilDocument.getConnectedRepository().getId(),
                tilDocument.getAnalysisJob().getId(),
                tilDocument.getTargetDate(),
                tilDocument.getTitle(),
                tilDocument.getContent(),
                tilDocument.getStatus(),
                toUtcOffsetDateTime(tilDocument.getCreatedAt()),
                toUtcOffsetDateTime(tilDocument.getUpdatedAt())
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