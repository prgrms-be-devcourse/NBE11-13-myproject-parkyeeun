package com.repoary.backend.til.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gemini TIL 미리보기 응답")
public record AiTilPreviewResponse(
        String title,
        String content
) {
}
