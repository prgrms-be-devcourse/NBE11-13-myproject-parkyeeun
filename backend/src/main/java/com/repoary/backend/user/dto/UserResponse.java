package com.repoary.backend.user.dto;

import com.repoary.backend.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "로그인 사용자 정보 응답")
public record UserResponse(

        @Schema(
                description = "Repoary 사용자 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "GitHub 사용자 고유 ID",
                example = "12345678"
        )
        Long githubId,

        @Schema(
                description = "GitHub 로그인 ID",
                example = "test-user"
        )
        String githubLogin,

        @Schema(
                description = "Repoary 계정 생성 시각",
                example = "2026-01-01T12:00:00"
        )
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getGithubId(),
                user.getGithubLogin(),
                user.getCreatedAt()
        );
    }
}