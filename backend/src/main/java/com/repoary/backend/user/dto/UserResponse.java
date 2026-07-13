package com.repoary.backend.user.dto;

import com.repoary.backend.user.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long githubId,
        String githubLogin,
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