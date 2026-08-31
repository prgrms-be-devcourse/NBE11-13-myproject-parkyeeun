package com.repoary.backend.til.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TIL Markdown 수정 요청")
public record TilUpdateRequest(

        @Schema(
                description = "수정할 TIL Markdown",
                example = "# 2026-08-28 TIL (Today I Learned)"
        )
        String content
) {
}