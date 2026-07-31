package com.repoary.backend.github.service;

import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.github.dto.GitHubCommitDetailResponse;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class GitHubCommitService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime DAY_BOUNDARY = LocalTime.of(6, 0);

    private final GitHubApiClient gitHubApiClient;
    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;

    public GitHubCommitService(
            GitHubApiClient gitHubApiClient,
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
    }

    @Transactional(readOnly = true)
    public List<GitHubCommitResponse> getCommits(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        RepositoryContext context = getRepositoryContext(
                userId,
                connectedRepositoryId
        );

        Instant since = targetDate
                .atTime(DAY_BOUNDARY)
                .atZone(KST)
                .toInstant();

        Instant until = targetDate
                .plusDays(1)
                .atTime(DAY_BOUNDARY)
                .atZone(KST)
                .toInstant();

        return gitHubApiClient.getCommits(
                context.user().getGithubAccessToken(),
                context.owner(),
                context.repositoryName(),
                context.repository().getDefaultBranch(),
                since,
                until
        );
    }

    @Transactional(readOnly = true)
    public List<GitHubCommitResponse> getCommits(
            Long userId,
            Long connectedRepositoryId,
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "분석 시작일과 종료일은 필수입니다."
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "분석 시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        RepositoryContext context = getRepositoryContext(
                userId,
                connectedRepositoryId
        );

        Instant since = from
                .atTime(DAY_BOUNDARY)
                .atZone(KST)
                .toInstant();

        Instant until = to
                .plusDays(1)
                .atTime(DAY_BOUNDARY)
                .atZone(KST)
                .toInstant();

        return gitHubApiClient.getCommits(
                context.user().getGithubAccessToken(),
                context.owner(),
                context.repositoryName(),
                context.repository().getDefaultBranch(),
                since,
                until
        );
    }

    @Transactional(readOnly = true)
    public GitHubCommitDetailResponse getCommitDetail(
            Long userId,
            Long connectedRepositoryId,
            String commitSha
    ) {
        if (commitSha == null || commitSha.isBlank()) {
            throw new IllegalArgumentException("커밋 SHA는 필수입니다.");
        }

        RepositoryContext context = getRepositoryContext(
                userId,
                connectedRepositoryId
        );

        return gitHubApiClient.getCommitDetail(
                context.user().getGithubAccessToken(),
                context.owner(),
                context.repositoryName(),
                commitSha
        );
    }

    private RepositoryContext getRepositoryContext(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );

        validateAccessToken(user);

        ConnectedRepository repository = connectedRepositoryRepository
                .findByIdAndUser(connectedRepositoryId, user)
                .orElseThrow(() ->
                        new IllegalArgumentException("연결된 저장소를 찾을 수 없습니다.")
                );

        String[] repositoryName = repository.getFullName().split("/", 2);

        if (repositoryName.length != 2
                || repositoryName[0].isBlank()
                || repositoryName[1].isBlank()) {
            throw new IllegalStateException(
                    "GitHub 저장소 이름 형식이 올바르지 않습니다."
            );
        }

        return new RepositoryContext(
                user,
                repository,
                repositoryName[0],
                repositoryName[1]
        );
    }

    private void validateAccessToken(User user) {
        if (user.getGithubAccessToken() == null
                || user.getGithubAccessToken().isBlank()) {
            throw new IllegalStateException(
                    "GitHub access token이 없습니다."
            );
        }
    }

    private record RepositoryContext(
            User user,
            ConnectedRepository repository,
            String owner,
            String repositoryName
    ) {
    }
}