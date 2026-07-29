package com.repoary.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "GitHub 저장소 응답")
public record GitHubRepositoryResponse(

        @Schema(
                description = "GitHub 저장소 고유 ID",
                example = "123456789"
        )
        Long id,

        @Schema(
                description = "저장소 이름",
                example = "sample-repository"
        )
        String name,

        @Schema(
                description = "소유자를 포함한 저장소 전체 이름",
                example = "test-user/sample-repository"
        )
        @JsonAlias("full_name")
        String fullName,

        @Schema(
                description = "GitHub 저장소 페이지 URL",
                example = "https://github.com/test-user/sample-repository"
        )
        @JsonAlias("html_url")
        String htmlUrl,

        @Schema(
                description = "비공개 저장소 여부",
                example = "false"
        )
        @JsonAlias("private")
        boolean privateRepository,

        @Schema(
                description = "저장소 기본 브랜치",
                example = "main"
        )
        @JsonAlias("default_branch")
        String defaultBranch
) {
}