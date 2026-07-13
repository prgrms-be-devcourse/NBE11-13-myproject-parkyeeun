package com.repoary.backend.auth.service;

import com.repoary.backend.auth.client.GitHubOAuthClient;
import com.repoary.backend.auth.config.GitHubOAuthProperties;
import com.repoary.backend.auth.dto.GitHubAccessTokenRequest;
import com.repoary.backend.auth.dto.GitHubAccessTokenResponse;
import com.repoary.backend.auth.dto.GitHubUserResponse;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final GitHubOAuthClient gitHubOAuthClient;
    private final UserRepository userRepository;

    public AuthService(
            GitHubOAuthProperties gitHubOAuthProperties,
            GitHubOAuthClient gitHubOAuthClient,
            UserRepository userRepository
    ) {
        this.gitHubOAuthProperties = gitHubOAuthProperties;
        this.gitHubOAuthClient = gitHubOAuthClient;
        this.userRepository = userRepository;
    }

    @Transactional
    public User loginWithGitHub(String code) {
        GitHubAccessTokenRequest request = new GitHubAccessTokenRequest(
                gitHubOAuthProperties.clientId(),
                gitHubOAuthProperties.clientSecret(),
                code,
                gitHubOAuthProperties.redirectUri()
        );

        GitHubAccessTokenResponse tokenResponse = gitHubOAuthClient.requestAccessToken(request);
        GitHubUserResponse gitHubUser = gitHubOAuthClient.requestUserInfo(tokenResponse.accessToken());

        return userRepository.findByGithubId(gitHubUser.id())
                .orElseGet(() -> userRepository.save(
                        new User(gitHubUser.id(), gitHubUser.login())
                ));
    }
}