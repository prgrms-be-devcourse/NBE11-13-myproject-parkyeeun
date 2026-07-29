package com.repoary.backend.github.client;

import com.repoary.backend.github.dto.GitHubContentResponse;
import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GitHubApiClient {

    private static final String GITHUB_API_VERSION = "2026-03-10";

    private final RestClient restClient;

    public GitHubApiClient() {
        this.restClient = RestClient.create("https://api.github.com");
    }

    public List<GitHubRepositoryResponse> getRepositories(String accessToken) {
        List<GitHubRepositoryResponse> repositories = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", 100)
                        .build())
                .headers(headers -> setGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return repositories == null ? List.of() : repositories;
    }

    public List<String> getRootDirectoryNames(
            String accessToken,
            String owner,
            String repositoryName,
            String defaultBranch
    ) {
        List<GitHubContentResponse> contents = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repository}/contents")
                        .queryParam("ref", defaultBranch)
                        .build(owner, repositoryName))
                .headers(headers -> setGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (contents == null) {
            return List.of();
        }

        return contents.stream()
                .filter(GitHubContentResponse::isDirectory)
                .map(GitHubContentResponse::name)
                .toList();
    }

    private void setGitHubHeaders(
            org.springframework.http.HttpHeaders headers,
            String accessToken
    ) {
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
    }
}