package com.repoary.backend.repository.dto;

import com.repoary.backend.repository.domain.ConnectedRepository;

import java.time.LocalDateTime;

public record ConnectedRepositoryResponse(
        Long id,
        Long githubRepositoryId,
        String name,
        String fullName,
        String htmlUrl,
        boolean privateRepository,
        String defaultBranch,
        LocalDateTime connectedAt
) {

    public static ConnectedRepositoryResponse from(ConnectedRepository repository) {
        return new ConnectedRepositoryResponse(
                repository.getId(),
                repository.getGithubRepositoryId(),
                repository.getName(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                repository.isPrivateRepository(),
                repository.getDefaultBranch(),
                repository.getConnectedAt()
        );
    }
}