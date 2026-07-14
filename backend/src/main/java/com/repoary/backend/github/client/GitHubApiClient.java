package com.repoary.backend.github.client;

import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GitHubApiClient {

    private final RestClient restClient;

    public GitHubApiClient() {
        this.restClient = RestClient.create("https://api.github.com");
    }

    public List<GitHubRepositoryResponse> getRepositories(String accessToken) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", 100)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}