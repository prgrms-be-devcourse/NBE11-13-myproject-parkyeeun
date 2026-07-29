package com.repoary.backend.rule.dto;

import com.repoary.backend.rule.domain.ClassificationRule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "경로 분류 규칙 응답")
public record ClassificationRuleResponse(

        @Schema(description = "경로 분류 규칙 ID", example = "1")
        Long id,

        @Schema(
                description = "분류할 파일 경로 패턴",
                example = "backend/src/main/java/**"
        )
        String pathPattern,

        @Schema(description = "분류 카테고리", example = "practice")
        String category,

        @Schema(description = "세부 분류 범위", example = "springboot")
        String scope,

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

    public static ClassificationRuleResponse from(
            ClassificationRule rule
    ) {
        return new ClassificationRuleResponse(
                rule.getId(),
                rule.getPathPattern(),
                rule.getCategory(),
                rule.getScope(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.isDefaultRule(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}