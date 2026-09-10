package com.repoary.backend.github.client;

import com.repoary.backend.github.dto.GitHubCommitDetailResponse;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.dto.GitHubContentResponse;
import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public List<GitHubCommitResponse> getCommits(
            String accessToken,
            String owner,
            String repositoryName,
            String defaultBranch,
            Instant since,
            Instant until
    ) {
        List<GitHubCommitResponse> allCommits = new ArrayList<>();

        int page = 1;
        int perPage = 100;

        while (true) {
            int currentPage = page;

            List<GitHubCommitResponse> commits = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repository}/commits")
                            .queryParam("sha", defaultBranch)
                            .queryParam("since", since.toString())
                            .queryParam("until", until.toString())
                            .queryParam("per_page", perPage)
                            .queryParam("page", currentPage)
                            .build(owner, repositoryName))
                    .headers(headers ->
                            setGitHubHeaders(headers, accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (commits == null || commits.isEmpty()) {
                break;
            }

            allCommits.addAll(commits);

            if (commits.size() < perPage) {
                break;
            }

            page++;
        }

        return allCommits;
    }

    private void setGitHubHeaders(
            org.springframework.http.HttpHeaders headers,
            String accessToken
    ) {
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
    }

    public GitHubCommitDetailResponse getCommitDetail(
            String accessToken,
            String owner,
            String repositoryName,
            String commitSha
    ) {
        GitHubCommitDetailResponse response = restClient.get()
                .uri(
                        "/repos/{owner}/{repository}/commits/{commitSha}",
                        owner,
                        repositoryName,
                        commitSha
                )
                .headers(headers -> setGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(GitHubCommitDetailResponse.class);

        if (response == null) {
            throw new IllegalStateException("GitHub 커밋 상세 응답이 없습니다.");
        }

        return response;
    }

    public Optional<GitHubContentResponse> getContent(
            String accessToken,
            String owner,
            String repositoryName,
            String defaultBranch,
            String path
    ) {
        try {
            GitHubContentResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repository}/contents/")
                            .path(path)
                            .queryParam("ref", defaultBranch)
                            .build(owner, repositoryName))
                    .headers(headers ->
                            setGitHubHeaders(headers, accessToken))
                    .retrieve()
                    .body(GitHubContentResponse.class);

            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }
}