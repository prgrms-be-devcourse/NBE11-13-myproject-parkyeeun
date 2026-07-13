package com.repoary.backend.auth.dto;

import com.repoary.backend.user.domain.User;

public record LoginResponse(
        Long id,
        Long githubId,
        String githubLogin,
        String accessToken
) {

    public static LoginResponse of(User user, String accessToken) {
        return new LoginResponse(
                user.getId(),
                user.getGithubId(),
                user.getGithubLogin(),
                accessToken
        );
    }
}