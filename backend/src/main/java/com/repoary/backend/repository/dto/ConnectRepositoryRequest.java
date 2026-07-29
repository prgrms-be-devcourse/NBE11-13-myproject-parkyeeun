package com.repoary.backend.repository.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "GitHub 저장소 연결 요청")
public record ConnectRepositoryRequest(

        @Schema(
                description = "GitHub에서 발급한 저장소 고유 ID",
                example = "123456789"
        )
        Long githubRepositoryId,

        @Schema(
                description = "저장소 이름",
                example = "sample-repository"
        )
        String name,

        @Schema(
                description = "소유자를 포함한 저장소 전체 이름",
                example = "test-user/sample-repository"
        )
        String fullName,

        @Schema(
                description = "GitHub 저장소 페이지 URL",
                example = "URL: https://github.com/test-user/sample-repository"
        )
        String htmlUrl,

        @Schema(
                description = "비공개 저장소 여부",
                example = "false"
        )
        boolean privateRepository,

        @Schema(
                description = "저장소 기본 브랜치",
                example = "main"
        )
        String defaultBranch
) {
}