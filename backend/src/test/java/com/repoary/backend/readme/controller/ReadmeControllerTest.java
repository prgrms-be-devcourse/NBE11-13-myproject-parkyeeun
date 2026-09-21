package com.repoary.backend.readme.controller;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.common.exception.GlobalExceptionHandler;
import com.repoary.backend.readme.exception.ReadmeErrorCode;
import com.repoary.backend.readme.service.ReadmeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReadmeControllerTest {

    private ReadmeService readmeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        readmeService = mock(ReadmeService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReadmeController(readmeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("README 원본 TIL 형식이 올바르지 않으면 안전한 400 오류를 반환한다")
    void invalidTilFormatReturnsBadRequest() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 9, 10);
        when(readmeService.generateRow(1L, 10L, targetDate))
                .thenThrow(new BusinessException(
                        ReadmeErrorCode.INVALID_TIL_FORMAT
                ));

        var authentication = new UsernamePasswordAuthenticationToken(
                1L,
                null,
                List.of()
        );

        mockMvc.perform(post(
                        "/api/repositories/{connectedRepositoryId}/readme-rows",
                        10L
                )
                        .principal(authentication)
                        .queryParam("date", "2026-09-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("README_INVALID_TIL_FORMAT"))
                .andExpect(jsonPath("$.message")
                        .value("README 행을 생성할 수 있는 TIL 형식이 아닙니다."));
    }
}
