package com.repoary.backend.auth.dto;

public record GitHubUserResponse(
        Long id,
        String login
) {
}