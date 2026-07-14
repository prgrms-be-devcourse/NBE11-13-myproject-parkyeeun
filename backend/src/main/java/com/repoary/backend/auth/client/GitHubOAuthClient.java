package com.repoary.backend.auth.client;

import com.repoary.backend.auth.dto.GitHubAccessTokenRequest;
import com.repoary.backend.auth.dto.GitHubAccessTokenResponse;
import com.repoary.backend.auth.dto.GitHubUserResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GitHubOAuthClient {

    private final RestClient restClient;

    public GitHubOAuthClient() {
        this.restClient = RestClient.create("https://github.com");
    }

    public GitHubAccessTokenResponse requestAccessToken(GitHubAccessTokenRequest request) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", request.clientId());
        formData.add("client_secret", request.clientSecret());
        formData.add("code", request.code());
        formData.add("redirect_uri", request.redirectUri());

        return restClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Accept", "application/json")
                .body(formData)
                .retrieve()
                .body(GitHubAccessTokenResponse.class);
    }

    public GitHubUserResponse requestUserInfo(String accessToken) {
        RestClient apiClient = RestClient.create("https://api.github.com");

        return apiClient.get()
                .uri("/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GitHubUserResponse.class);
    }
}