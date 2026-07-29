package com.repoary.backend.repository.dto;

import com.repoary.backend.repository.domain.ConnectedRepository;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Repoary에 연결된 GitHub 저장소 응답")
public record ConnectedRepositoryResponse(

        @Schema(
                description = "Repoary 내부 연결 저장소 ID",
                example = "1"
        )
        Long id,

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
                example = "https://github.com/test-user/sample-repository"
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
        String defaultBranch,

        @Schema(
                description = "Repoary에 저장소를 연결한 시각",
                example = "2026-01-01T12:00:00"
        )
        LocalDateTime connectedAt
) {

    public static ConnectedRepositoryResponse from(ConnectedRepository repository) {
        return new ConnectedRepositoryResponse(
                repository.getId(),
                repository.getGithubRepositoryId(),
                repository.getName(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                repository.isPrivateRepository(),
                repository.getDefaultBranch(),
                repository.getConnectedAt()
        );
    }
}