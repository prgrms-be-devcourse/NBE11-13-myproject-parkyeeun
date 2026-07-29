package com.repoary.backend.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "경로 분류 규칙 요청")
public record ClassificationRuleRequest(

        @Schema(
                description = "분류할 파일 경로 패턴",
                example = "backend/src/main/java/**"
        )
        String pathPattern,

        @Schema(
                description = "분류 카테고리",
                example = "practice"
        )
        String category,

        @Schema(
                description = "세부 분류 범위",
                example = "springboot"
        )
        String scope,

        @Schema(
                description = "규칙 적용 우선순위. 값이 낮을수록 우선 적용된다.",
                example = "1"
        )
        Integer priority
) {
}