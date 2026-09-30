package com.repoary.backend.til.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AiTilDraftRequestTest {

    @Test
    @DisplayName("요청 객체 문자열에 API Key를 노출하지 않는다")
    void apiKeyIsRedactedFromToString() {
        AiTilDraftRequest request = new AiTilDraftRequest(
                LocalDate.of(2026, 9, 28),
                "sensitive-api-key"
        );

        assertThat(request.toString())
                .doesNotContain("sensitive-api-key")
                .contains("apiKey=***");
    }

    @Test
    @DisplayName("API Key는 역직렬화되지만 JSON 응답에는 직렬화되지 않는다")
    void apiKeyIsWriteOnly() throws Exception {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        AiTilDraftRequest request = jsonMapper.readValue(
                """
                        {
                          "date": "2026-09-28",
                          "apiKey": "sensitive-api-key"
                        }
                        """,
                AiTilDraftRequest.class
        );

        assertThat(request.apiKey()).isEqualTo("sensitive-api-key");
        assertThat(jsonMapper.writeValueAsString(request))
                .doesNotContain("sensitive-api-key", "apiKey")
                .contains("2026-09-28");
    }
}
