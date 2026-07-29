package com.repoary.backend.github.controller;

import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import com.repoary.backend.github.service.GitHubRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "GitHub Repository",
        description = "GitHub 저장소 조회 API"
)
@RestController
public class GitHubRepositoryController {

    private final GitHubRepositoryService gitHubRepositoryService;

    public GitHubRepositoryController(
            GitHubRepositoryService gitHubRepositoryService
    ) {
        this.gitHubRepositoryService = gitHubRepositoryService;
    }

    @Operation(
            summary = "GitHub 저장소 목록 조회",
            description = "로그인한 사용자의 GitHub 계정에서 접근 가능한 저장소 목록을 조회한다."
    )
    @GetMapping("/api/github/repositories")
    public List<GitHubRepositoryResponse> getRepositories(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return gitHubRepositoryService.getRepositories(userId);
    }
}