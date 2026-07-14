package com.repoary.backend.github.controller;

import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import com.repoary.backend.github.service.GitHubRepositoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GitHubRepositoryController {

    private final GitHubRepositoryService gitHubRepositoryService;

    public GitHubRepositoryController(GitHubRepositoryService gitHubRepositoryService) {
        this.gitHubRepositoryService = gitHubRepositoryService;
    }

    @GetMapping("/api/github/repositories")
    public List<GitHubRepositoryResponse> getRepositories(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        return gitHubRepositoryService.getRepositories(userId);
    }
}