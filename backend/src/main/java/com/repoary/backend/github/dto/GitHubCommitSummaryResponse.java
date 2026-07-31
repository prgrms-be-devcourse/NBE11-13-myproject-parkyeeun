package com.repoary.backend.github.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "GitHub 커밋 요약 응답")
public record GitHubCommitSummaryResponse(

        @Schema(
                description = "커밋 SHA",
                example = "a1b2c3d4e5f6"
        )
        String sha,

        @Schema(
                description = "커밋 메시지",
                example = "Feat: 날짜별 커밋 조회 기능 구현"
        )
        String message,

        @Schema(
                description = "GitHub 커밋 페이지 URL",
                example = "https://github.com/test-user/sample-repository/commit/a1b2c3d4e5f6"
        )
        String htmlUrl,

        @Schema(
                description = "커밋 시각",
                example = "2026-07-30T17:30:00Z"
        )
        Instant committedAt
) {

    public static GitHubCommitSummaryResponse from(
            GitHubCommitResponse response
    ) {
        return new GitHubCommitSummaryResponse(
                response.sha(),
                response.message(),
                response.htmlUrl(),
                response.committedAt()
        );
    }
}