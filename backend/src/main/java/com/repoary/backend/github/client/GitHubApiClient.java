package com.repoary.backend.github.client;

import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.github.dto.GitHubCommitDetailResponse;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.dto.GitHubContentResponse;
import com.repoary.backend.github.dto.GitHubRepositoryResponse;
import com.repoary.backend.github.exception.GitHubErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class GitHubApiClient {

    private static final String GITHUB_API_VERSION = "2026-03-10";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

    private final RestClient restClient;

    public GitHubApiClient() {
        this(createBuilder().baseUrl("https://api.github.com"));
    }

    private static RestClient.Builder createBuilder() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    GitHubApiClient(RestClient.Builder builder) {
        this.restClient = builder
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        GitHubApiClient::handleErrorStatus
                )
                .build();
    }

    public List<GitHubRepositoryResponse> getRepositories(String accessToken) {
        List<GitHubRepositoryResponse> repositories = execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", 100)
                        .build())
                .headers(headers -> setGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));

        if (repositories == null) {
            throw invalidResponse();
        }
        return repositories;
    }

    public List<String> getRootDirectoryNames(
            String accessToken,
            String owner,
            String repositoryName,
            String defaultBranch
    ) {
        List<GitHubContentResponse> contents = execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repository}/contents")
                        .queryParam("ref", defaultBranch)
                        .build(owner, repositoryName))
                .headers(headers -> setGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));

        if (contents == null) {
            throw invalidResponse();
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

            List<GitHubCommitResponse> commits = execute(() -> restClient.get()
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
                    }));

            if (commits == null) {
                throw invalidResponse();
            }

            validateCommits(commits);

            if (commits.isEmpty()) {
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
        GitHubCommitDetailResponse response = execute(() -> restClient.get()
                .uri(
                        "/repos/{owner}/{repository}/commits/{commitSha}",
                        owner,
                        repositoryName,
                        commitSha
                )
                .headers(headers -> setGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(GitHubCommitDetailResponse.class));

        if (response == null) {
            throw invalidResponse();
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
            GitHubContentResponse response = execute(() -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repository}/contents/")
                            .path(path)
                            .queryParam("ref", defaultBranch)
                            .build(owner, repositoryName))
                    .headers(headers ->
                            setGitHubHeaders(headers, accessToken))
                    .retrieve()
                    .body(GitHubContentResponse.class));

            if (response == null) {
                throw invalidResponse();
            }
            return Optional.of(response);
        } catch (ExternalSystemException exception) {
            if (exception.getErrorCode() == GitHubErrorCode.RESOURCE_NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    public Optional<List<GitHubContentResponse>> getDirectoryContents(
            String accessToken,
            String owner,
            String repositoryName,
            String defaultBranch,
            String path
    ) {
        try {
            List<GitHubContentResponse> response = execute(() -> restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repository}/contents/")
                            .path(path)
                            .queryParam("ref", defaultBranch)
                            .build(owner, repositoryName))
                    .headers(headers ->
                            setGitHubHeaders(headers, accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    }));

            if (response == null) {
                throw invalidResponse();
            }
            return Optional.of(response);
        } catch (ExternalSystemException exception) {
            if (exception.getErrorCode() == GitHubErrorCode.RESOURCE_NOT_FOUND) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private <T> T execute(Supplier<T> request) {
        try {
            return request.get();
        } catch (ExternalSystemException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            GitHubErrorCode errorCode = hasTimeoutCause(exception)
                    ? GitHubErrorCode.TIMEOUT
                    : GitHubErrorCode.CONNECTION_FAILED;
            throw new ExternalSystemException(errorCode, exception);
        } catch (RestClientException exception) {
            throw new ExternalSystemException(
                    GitHubErrorCode.INVALID_RESPONSE,
                    exception
            );
        }
    }

    private ExternalSystemException invalidResponse() {
        return new ExternalSystemException(
                GitHubErrorCode.INVALID_RESPONSE,
                null
        );
    }

    private void validateCommits(List<GitHubCommitResponse> commits) {
        boolean invalid = commits.stream().anyMatch(commit ->
                commit == null
                        || commit.commit() == null
                        || commit.commit().committer() == null
                        || commit.commit().committer().date() == null
        );

        if (invalid) {
            throw invalidResponse();
        }
    }

    private static void handleErrorStatus(
            org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response
    ) throws IOException {
        int status = response.getStatusCode().value();
        GitHubErrorCode errorCode;

        if (status == 401) {
            errorCode = GitHubErrorCode.AUTHENTICATION_FAILED;
        } else if (status == 403) {
            errorCode = isRateLimited(response.getHeaders())
                    ? GitHubErrorCode.RATE_LIMIT_EXCEEDED
                    : GitHubErrorCode.ACCESS_DENIED;
        } else if (status == 404) {
            errorCode = GitHubErrorCode.RESOURCE_NOT_FOUND;
        } else if (status == 429) {
            errorCode = GitHubErrorCode.RATE_LIMIT_EXCEEDED;
        } else if (status >= 500) {
            errorCode = GitHubErrorCode.API_UNAVAILABLE;
        } else {
            errorCode = GitHubErrorCode.INVALID_RESPONSE;
        }

        throw new ExternalSystemException(errorCode, null);
    }

    private static boolean isRateLimited(HttpHeaders headers) {
        return "0".equals(headers.getFirst("X-RateLimit-Remaining"))
                || headers.getFirst(HttpHeaders.RETRY_AFTER) != null;
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
