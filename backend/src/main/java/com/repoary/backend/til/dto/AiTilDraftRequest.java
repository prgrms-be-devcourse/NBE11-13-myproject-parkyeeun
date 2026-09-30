package com.repoary.backend.til.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Gemini를 사용한 TIL 초안 생성 요청")
public record AiTilDraftRequest(
        LocalDate date,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String apiKey
) {

    @Override
    public String toString() {
        return "AiTilDraftRequest[date=" + date + ", apiKey=***]";
    }
}
