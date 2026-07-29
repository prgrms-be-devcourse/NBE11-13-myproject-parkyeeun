package com.repoary.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "GitHub OAuth 로그인 URL 응답")
public record GitHubLoginUrlResponse(

        @Schema(
                description = "GitHub OAuth 인증 페이지 URL",
                example = "https://github.com/login/oauth/authorize?client_id=client-id&redirect_uri=http://localhost:8080/api/auth/github/callback&scope=repo"
        )
        String loginUrl
) {
}