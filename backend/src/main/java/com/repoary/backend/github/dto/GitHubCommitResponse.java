package com.repoary.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitResponse(
        String sha,

        @JsonAlias("html_url")
        String htmlUrl,

        CommitInfo commit
) {

    public String message() {
        return commit.message();
    }

    public Instant committedAt() {
        return commit.committer().date();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitInfo(
            String message,
            GitUserInfo author,
            GitUserInfo committer
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitUserInfo(
            String name,
            String email,
            Instant date
    ) {
    }
}