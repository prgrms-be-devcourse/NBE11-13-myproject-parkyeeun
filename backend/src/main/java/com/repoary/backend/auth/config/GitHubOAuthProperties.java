package com.repoary.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.oauth")
public record GitHubOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {
}