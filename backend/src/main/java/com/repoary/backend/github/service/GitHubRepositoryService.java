package com.repoary.backend.github.service;

import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GitHubRepositoryService {

    private final GitHubApiClient gitHubApiClient;
    private final UserRepository userRepository;

    public GitHubRepositoryService(
            GitHubApiClient gitHubApiClient,
            UserRepository userRepository
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GitHubRepositoryResponse> getRepositories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            throw new IllegalStateException("GitHub access token이 없습니다.");
        }

        return gitHubApiClient.getRepositories(user.getGithubAccessToken());
    }
}