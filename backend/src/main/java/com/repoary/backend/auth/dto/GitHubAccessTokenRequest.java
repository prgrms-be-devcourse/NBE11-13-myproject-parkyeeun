package com.repoary.backend.auth.dto;

public record GitHubAccessTokenRequest(
        String clientId,
        String clientSecret,
        String code,
        String redirectUri
) {
}