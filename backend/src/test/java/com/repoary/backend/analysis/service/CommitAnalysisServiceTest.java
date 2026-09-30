package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
import com.repoary.backend.analysis.dto.ConventionMatchResult;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.github.dto.GitHubCommitDetailResponse;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.service.GitHubCommitService;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitAnalysisServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long REPOSITORY_ID = 10L;
    private static final LocalDate TARGET_DATE =
            LocalDate.of(2026, 9, 30);

    @Mock
    private GitHubCommitService gitHubCommitService;

    @Mock
    private RepositoryRuleMatcher repositoryRuleMatcher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectedRepositoryRepository connectedRepositoryRepository;

    @Mock
    private ClassificationRuleRepository classificationRuleRepository;

    @Mock
    private ConventionRuleRepository conventionRuleRepository;

    @Mock
    private User user;

    @Mock
    private ConnectedRepository connectedRepository;

    private CommitAnalysisService commitAnalysisService;

    @BeforeEach
    void setUp() {
        commitAnalysisService = new CommitAnalysisService(
                gitHubCommitService,
                repositoryRuleMatcher,
                new ConventionalCommitParser(),
                userRepository,
                connectedRepositoryRepository,
                classificationRuleRepository,
                conventionRuleRepository
        );

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(connectedRepositoryRepository.findByIdAndUser(
                REPOSITORY_ID,
                user
        )).thenReturn(Optional.of(connectedRepository));
        when(classificationRuleRepository
                .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                        connectedRepository
                )).thenReturn(List.of());
        when(conventionRuleRepository
                .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                        connectedRepository
                )).thenReturn(List.of());
    }

    @Test
    @DisplayName("사용자 convention 규칙이 매칭되면 fallback이 덮어쓰지 않는다")
    void preserveMatchedUserConventionRule() {
        ConventionMatchResult matched = new ConventionMatchResult(
                7L,
                "chore(deploy):",
                "custom-type",
                "custom-scope",
                "custom-category",
                10
        );
        stubCommit("chore(deploy): GitHub Actions EC2 배포 워크플로 추가");
        when(repositoryRuleMatcher.matchConventionRule(
                anyString(),
                anyList()
        )).thenReturn(Optional.of(matched));

        CommitAnalysisResponse result = analyzeSingle();

        assertThat(result.convention()).isSameAs(matched);
    }

    @Test
    @DisplayName("규칙 미매칭 Conventional Commit에서 type과 scope를 추출한다")
    void fallbackToConventionalCommitTypeAndScope() {
        stubCommit("chore(deploy): GitHub Actions EC2 배포 워크플로 추가");
        stubNoConventionRuleMatch();

        CommitAnalysisResponse result = analyzeSingle();

        assertThat(result.convention()).isNotNull();
        assertThat(result.convention().commitType()).isEqualTo("chore");
        assertThat(result.convention().scope()).isEqualTo("deploy");
        assertThat(result.convention().category()).isNull();
        assertThat(result.convention().ruleId()).isNull();
        assertThat(result.convention().messagePattern()).isNull();
    }

    @Test
    @DisplayName("scope 없는 Conventional Commit은 type만 추출한다")
    void fallbackToConventionalCommitWithoutScope() {
        stubCommit("chore: 빌드 설정 정리");
        stubNoConventionRuleMatch();

        CommitAnalysisResponse result = analyzeSingle();

        assertThat(result.convention()).isNotNull();
        assertThat(result.convention().commitType()).isEqualTo("chore");
        assertThat(result.convention().scope()).isNull();
        assertThat(result.convention().category()).isNull();
    }

    @Test
    @DisplayName("Conventional Commit 형식이 아니면 기존처럼 convention이 비어 있다")
    void keepNonConventionalCommitUnclassified() {
        stubCommit("GitHub Actions EC2 배포 워크플로 추가");
        stubNoConventionRuleMatch();

        CommitAnalysisResponse result = analyzeSingle();

        assertThat(result.convention()).isNull();
    }

    @Test
    @DisplayName("fallback type과 scope를 기존 분석 결과 형식으로 저장한다")
    void storeFallbackInExistingAnalysisResultShape() {
        stubCommit("chore(deploy): GitHub Actions EC2 배포 워크플로 추가");
        stubNoConventionRuleMatch();
        CommitAnalysisResponse analysis = analyzeSingle();
        StoredAnalysisResultMapper mapper =
                new StoredAnalysisResultMapper(new AnalysisFileFilter());

        StoredAnalysisResult stored = mapper.map(
                TARGET_DATE,
                List.of(analysis)
        );

        assertThat(stored.commits()).hasSize(1);
        StoredAnalysisResult.StoredCommitAnalysis commit =
                stored.commits().get(0);
        assertThat(commit.commitType()).isEqualTo("chore");
        assertThat(commit.scope()).isEqualTo("deploy");
        assertThat(commit.categories()).isEmpty();
    }

    private void stubCommit(String message) {
        GitHubCommitResponse commit = new GitHubCommitResponse(
                "commit-sha",
                "https://github.com/owner/repository/commit/commit-sha",
                new GitHubCommitResponse.CommitInfo(
                        message,
                        null,
                        new GitHubCommitResponse.GitUserInfo(
                                "author",
                                "author@example.com",
                                Instant.parse("2026-09-30T01:00:00Z")
                        )
                )
        );
        when(gitHubCommitService.getCommits(
                USER_ID,
                REPOSITORY_ID,
                TARGET_DATE
        )).thenReturn(List.of(commit));
        when(gitHubCommitService.getCommitDetail(
                USER_ID,
                REPOSITORY_ID,
                "commit-sha"
        )).thenReturn(new GitHubCommitDetailResponse(
                "commit-sha",
                commit.htmlUrl(),
                List.of()
        ));
    }

    private void stubNoConventionRuleMatch() {
        when(repositoryRuleMatcher.matchConventionRule(
                anyString(),
                anyList()
        )).thenReturn(Optional.empty());
    }

    private CommitAnalysisResponse analyzeSingle() {
        return commitAnalysisService.analyzeCommits(
                USER_ID,
                REPOSITORY_ID,
                TARGET_DATE
        ).get(0);
    }
}
