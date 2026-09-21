package com.repoary.backend.github.service;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import com.repoary.backend.github.exception.GitHubErrorCode;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.exception.UserErrorCode;
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
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getGithubAccessToken() == null || user.getGithubAccessToken().isBlank()) {
            throw new BusinessException(GitHubErrorCode.ACCESS_TOKEN_MISSING);
        }

        return gitHubApiClient.getRepositories(user.getGithubAccessToken());
    }
}
