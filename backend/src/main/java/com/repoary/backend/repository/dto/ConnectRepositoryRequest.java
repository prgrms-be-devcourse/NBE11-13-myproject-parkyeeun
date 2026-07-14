package com.repoary.backend.repository.dto;

public record ConnectRepositoryRequest(
        Long githubRepositoryId,
        String name,
        String fullName,
        String htmlUrl,
        boolean privateRepository,
        String defaultBranch
) {
}