package com.repoary.backend.rule.dto;

import com.repoary.backend.rule.domain.ConventionRule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "커밋 규칙 응답")
public record ConventionRuleResponse(

        @Schema(description = "커밋 규칙 ID", example = "1")
        Long id,

        @Schema(
                description = "커밋 메시지 판별 패턴",
                example = "Feat:"
        )
        String messagePattern,

        @Schema(
                description = "판별된 커밋 유형",
                example = "Feat"
        )
        String commitType,

        @Schema(description = "커밋 범위", example = "backend")
        String scope,

        @Schema(description = "분류 카테고리", example = "practice")
        String category,

        @Schema(
                description = "규칙 적용 우선순위. 값이 낮을수록 우선 적용된다.",
                example = "1"
        )
        Integer priority,

        @Schema(description = "규칙 활성화 여부", example = "true")
        boolean enabled,

        @Schema(description = "기본 규칙 여부", example = "false")
        boolean defaultRule,

        @Schema(
                description = "규칙 생성 시각",
                example = "2026-01-01T12:00:00"
        )
        LocalDateTime createdAt,

        @Schema(
                description = "규칙 수정 시각",
                example = "2026-07-01T12:00:00"
        )
        LocalDateTime updatedAt
) {

    public static ConventionRuleResponse from(
            ConventionRule rule
    ) {
        return new ConventionRuleResponse(
                rule.getId(),
                rule.getMessagePattern(),
                rule.getCommitType(),
                rule.getScope(),
                rule.getCategory(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.isDefaultRule(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}