package com.repoary.backend.auth.controller;

import com.repoary.backend.auth.config.GitHubOAuthProperties;
import com.repoary.backend.auth.dto.*;
import com.repoary.backend.auth.jwt.JwtProvider;
import com.repoary.backend.auth.service.AuthService;
import com.repoary.backend.user.domain.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class AuthController {

    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthController(
            GitHubOAuthProperties gitHubOAuthProperties,
            AuthService authService,
            JwtProvider jwtProvider
    ) {
        this.gitHubOAuthProperties = gitHubOAuthProperties;
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

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

    @GetMapping("/api/auth/github/callback")
    public LoginResponse githubCallback(@RequestParam String code) {
        User user = authService.loginWithGitHub(code);
        String accessToken = jwtProvider.createAccessToken(user);

        return LoginResponse.of(user, accessToken);
    }
}