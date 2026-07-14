package com.repoary.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GitHubRepositoryResponse(
        Long id,
        String name,

        @JsonAlias("full_name")
        String fullName,

        @JsonAlias("html_url")
        String htmlUrl,

        @JsonAlias("private")
        boolean privateRepository,

        @JsonAlias("default_branch")
        String defaultBranch
) {
}