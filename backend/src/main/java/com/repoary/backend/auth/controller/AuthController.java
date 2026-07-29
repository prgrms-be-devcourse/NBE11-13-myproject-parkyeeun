package com.repoary.backend.auth.controller;

import com.repoary.backend.auth.config.FrontendProperties;
import com.repoary.backend.auth.config.GitHubOAuthProperties;
import com.repoary.backend.auth.dto.GitHubLoginUrlResponse;
import com.repoary.backend.auth.jwt.JwtProvider;
import com.repoary.backend.auth.service.AuthService;
import com.repoary.backend.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Tag(
        name = "Authentication",
        description = "GitHub OAuth 로그인 API"
)
@RestController
public class AuthController {

    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final FrontendProperties frontendProperties;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthController(
            GitHubOAuthProperties gitHubOAuthProperties,
            FrontendProperties frontendProperties,
            AuthService authService,
            JwtProvider jwtProvider
    ) {
        this.gitHubOAuthProperties = gitHubOAuthProperties;
        this.frontendProperties = frontendProperties;
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    @Operation(
            summary = "GitHub 로그인 URL 조회",
            description = "GitHub OAuth 로그인을 시작하기 위한 인증 URL을 반환한다."
    )
    @GetMapping("/api/auth/github/login")
    public GitHubLoginUrlResponse githubLogin() {
        String loginUrl = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", gitHubOAuthProperties.clientId())
                .queryParam("redirect_uri", gitHubOAuthProperties.redirectUri())
                .queryParam("scope", "repo")
                .build()
                .toUriString();

        return new GitHubLoginUrlResponse(loginUrl);
    }

    @Operation(
            summary = "GitHub OAuth 콜백 처리",
            description = "GitHub에서 전달받은 인증 코드로 로그인한 뒤 JWT를 발급하고 프론트엔드로 리다이렉트한다."
    )
    @ApiResponse(
            responseCode = "302",
            description = "로그인 처리 후 프론트엔드로 리다이렉트"
    )
    @GetMapping("/api/auth/github/callback")
    public ResponseEntity<Void> githubCallback(
            @Parameter(
                    description = "GitHub OAuth 인증 코드",
                    example = "0123456789abcdef"
            )
            @RequestParam String code
    ) {
        User user = authService.loginWithGitHub(code);
        String accessToken = jwtProvider.createAccessToken(user);

        URI redirectUri = UriComponentsBuilder
                .fromUriString(frontendProperties.redirectUri())
                .queryParam("token", accessToken)
                .build()
                .toUri();

        return ResponseEntity
                .status(302)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }
}