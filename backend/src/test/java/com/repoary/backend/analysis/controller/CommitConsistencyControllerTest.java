package com.repoary.backend.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoary.backend.analysis.dto.CommitConsistencyResponse;
import com.repoary.backend.analysis.dto.ConsistencyCommitResponse;
import com.repoary.backend.analysis.dto.ConsistencyGroupResponse;
import com.repoary.backend.analysis.service.CommitConsistencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommitConsistencyControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CONNECTED_REPOSITORY_ID = 10L;

    private CommitConsistencyService commitConsistencyService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commitConsistencyService =
                mock(CommitConsistencyService.class);

        CommitConsistencyController controller =
                new CommitConsistencyController(
                        commitConsistencyService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("인증된 사용자는 기간별 커밋 일관성 분석을 요청할 수 있다")
    void analyzeConsistencyReturnsResult() throws Exception {
        LocalDate from =
                LocalDate.of(2026, 7, 1);

        LocalDate to =
                LocalDate.of(2026, 7, 31);

        ConsistencyCommitResponse commitResponse =
                new ConsistencyCommitResponse(
                        "abc123",
                        "study(java): 2026-07-30 컬렉션 실습",
                        "https://github.com/example/repository/commit/abc123",
                        Instant.parse(
                                "2026-07-30T10:00:00Z"
                        ),
                        "study",
                        "java",
                        "assignments",
                        true,
                        List.of()
                );

        ConsistencyGroupResponse groupResponse =
                new ConsistencyGroupResponse(
                        "assignments/**",
                        "assignments",
                        "java",
                        "study(java):",
                        1,
                        1,
                        0,
                        List.of(commitResponse)
                );

        CommitConsistencyResponse response =
                new CommitConsistencyResponse(
                        from,
                        to,
                        1,
                        1,
                        0,
                        List.of(groupResponse)
                );

        when(
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        from,
                        to
                )
        ).thenReturn(response);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        USER_ID,
                        null,
                        List.of()
                );

        String requestBody = """
                {
                  "from": "2026-07-01",
                  "to": "2026-07-31"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/repositories/{connectedRepositoryId}/consistency-analysis",
                                CONNECTED_REPOSITORY_ID
                        )
                                .principal(authentication)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.from")
                                .value("2026-07-01")
                )
                .andExpect(
                        jsonPath("$.to")
                                .value("2026-07-31")
                )
                .andExpect(
                        jsonPath("$.commitCount")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.consistentCount")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.inconsistentCount")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.groups[0].pathPattern")
                                .value("assignments/**")
                )
                .andExpect(
                        jsonPath("$.groups[0].category")
                                .value("assignments")
                )
                .andExpect(
                        jsonPath("$.groups[0].scope")
                                .value("java")
                )
                .andExpect(
                        jsonPath("$.groups[0].expectedPattern")
                                .value("study(java):")
                )
                .andExpect(
                        jsonPath("$.groups[0].commits[0].sha")
                                .value("abc123")
                )
                .andExpect(
                        jsonPath(
                                "$.groups[0].commits[0].consistent"
                        ).value(true)
                );

        verify(commitConsistencyService).analyze(
                USER_ID,
                CONNECTED_REPOSITORY_ID,
                from,
                to
        );
    }
}