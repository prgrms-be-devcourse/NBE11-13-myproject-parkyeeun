package com.repoary.backend.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커밋 규칙 요청")
public record ConventionRuleRequest(

        @Schema(
                description = "커밋 메시지 판별 패턴",
                example = "Feat:"
        )
        String messagePattern,

        @Schema(
                description = "판별할 커밋 유형",
                example = "Feat"
        )
        String commitType,

        @Schema(
                description = "커밋 범위",
                example = "backend"
        )
        String scope,

        @Schema(
                description = "분류 카테고리",
                example = "practice"
        )
        String category,

        @Schema(
                description = "규칙 적용 우선순위. 값이 낮을수록 우선 적용된다.",
                example = "1"
        )
        Integer priority
) {
}