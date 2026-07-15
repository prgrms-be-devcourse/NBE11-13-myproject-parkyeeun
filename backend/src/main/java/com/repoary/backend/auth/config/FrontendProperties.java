package com.repoary.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
        String redirectUri
) {
}