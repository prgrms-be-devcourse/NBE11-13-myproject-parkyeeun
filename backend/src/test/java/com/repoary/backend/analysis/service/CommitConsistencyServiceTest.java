package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.ClassificationMatchResult;
import com.repoary.backend.analysis.dto.CommitConsistencyResponse;
import com.repoary.backend.analysis.dto.ConsistencyCommitResponse;
import com.repoary.backend.analysis.dto.ConsistencyGroupResponse;
import com.repoary.backend.analysis.dto.ConventionMatchResult;
import com.repoary.backend.github.dto.GitHubCommitDetailResponse;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.service.GitHubCommitService;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitConsistencyServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CONNECTED_REPOSITORY_ID = 10L;

    private static final LocalDate FROM =
            LocalDate.of(2026, 7, 1);

    private static final LocalDate TO =
            LocalDate.of(2026, 7, 31);

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

    private CommitConsistencyService commitConsistencyService;

    @BeforeEach
    void setUp() {
        commitConsistencyService = new CommitConsistencyService(
                gitHubCommitService,
                repositoryRuleMatcher,
                userRepository,
                connectedRepositoryRepository,
                classificationRuleRepository,
                conventionRuleRepository
        );
    }

    @Test
    @DisplayName("같은 경로에서도 매칭된 커밋 규칙별로 그룹을 분리한다")
    void analyzeGroupsCommitsByMatchedConventionRule() {
        ClassificationRule classificationRule =
                new ClassificationRule(
                        connectedRepository,
                        "assignments/**",
                        "assignments",
                        null,
                        100,
                        true
                );

        ConventionRule docsRule =
                new ConventionRule(
                        connectedRepository,
                        "docs(assignments):",
                        "docs",
                        null,
                        "assignments",
                        100,
                        true
                );

        ConventionRule studyRule =
                new ConventionRule(
                        connectedRepository,
                        "study(java):",
                        "study",
                        "java",
                        null,
                        100,
                        true
                );

        ClassificationMatchResult classificationMatch =
                new ClassificationMatchResult(
                        1L,
                        "assignments/**",
                        "assignments",
                        null,
                        100
                );

        ConventionMatchResult docsMatch =
                new ConventionMatchResult(
                        2L,
                        "docs(assignments):",
                        "docs",
                        null,
                        "assignments",
                        100
                );

        ConventionMatchResult studyMatch =
                new ConventionMatchResult(
                        3L,
                        "study(java):",
                        "study",
                        "java",
                        null,
                        100
                );

        GitHubCommitResponse docsCommit =
                createCommit(
                        "sha-docs",
                        "docs(assignments): 2026-07-30 JWT 과제 문서 정리",
                        "2026-07-30T10:00:00Z"
                );

        GitHubCommitResponse studyCommit =
                createCommit(
                        "sha-study",
                        "study(java): 2026-07-30 JWT 과제 구현",
                        "2026-07-30T11:00:00Z"
                );

        GitHubCommitResponse unmatchedCommit =
                createCommit(
                        "sha-unmatched",
                        "update assignments",
                        "2026-07-30T12:00:00Z"
                );

        GitHubCommitResponse mergeCommit =
                createCommit(
                        "sha-merge",
                        "Merge branch 'feature/test'",
                        "2026-07-30T13:00:00Z"
                );

        stubOwnedRepository();

        when(
                classificationRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        )
        ).thenReturn(List.of(classificationRule));

        when(
                conventionRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        )
        ).thenReturn(List.of(docsRule, studyRule));

        when(
                gitHubCommitService.getCommits(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                )
        ).thenReturn(
                List.of(
                        docsCommit,
                        studyCommit,
                        unmatchedCommit,
                        mergeCommit
                )
        );

        when(
                gitHubCommitService.getCommitDetail(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        "sha-docs"
                )
        ).thenReturn(
                createDetail(
                        "sha-docs",
                        "assignments/springboot/token/README.md"
                )
        );

        when(
                gitHubCommitService.getCommitDetail(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        "sha-study"
                )
        ).thenReturn(
                createDetail(
                        "sha-study",
                        "assignments/springboot/token/src/Main.java"
                )
        );

        when(
                gitHubCommitService.getCommitDetail(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        "sha-unmatched"
                )
        ).thenReturn(
                createDetail(
                        "sha-unmatched",
                        "assignments/springboot/token/build.gradle"
                )
        );

        when(
                repositoryRuleMatcher.matchClassificationRule(
                        eq("assignments/springboot/token/README.md"),
                        anyList()
                )
        ).thenReturn(Optional.of(classificationMatch));

        when(
                repositoryRuleMatcher.matchClassificationRule(
                        eq("assignments/springboot/token/src/Main.java"),
                        anyList()
                )
        ).thenReturn(Optional.of(classificationMatch));

        when(
                repositoryRuleMatcher.matchClassificationRule(
                        eq("assignments/springboot/token/build.gradle"),
                        anyList()
                )
        ).thenReturn(Optional.of(classificationMatch));

        when(
                repositoryRuleMatcher.matchConventionRule(
                        eq(docsCommit.message()),
                        anyList()
                )
        ).thenReturn(Optional.of(docsMatch));

        when(
                repositoryRuleMatcher.matchConventionRule(
                        eq(studyCommit.message()),
                        anyList()
                )
        ).thenReturn(Optional.of(studyMatch));

        when(
                repositoryRuleMatcher.matchConventionRule(
                        eq(unmatchedCommit.message()),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        CommitConsistencyResponse response =
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                );

        assertThat(response.from()).isEqualTo(FROM);
        assertThat(response.to()).isEqualTo(TO);
        assertThat(response.commitCount()).isEqualTo(3);
        assertThat(response.consistentCount()).isEqualTo(2);
        assertThat(response.inconsistentCount()).isEqualTo(1);
        assertThat(response.groups()).hasSize(3);

        ConsistencyGroupResponse docsGroup =
                findGroup(
                        response,
                        "docs(assignments):"
                );

        assertThat(docsGroup.pathPattern())
                .isEqualTo("assignments/**");

        assertThat(docsGroup.category())
                .isEqualTo("assignments");

        assertThat(docsGroup.scope()).isNull();
        assertThat(docsGroup.commitCount()).isEqualTo(1);
        assertThat(docsGroup.consistentCount()).isEqualTo(1);
        assertThat(docsGroup.inconsistentCount()).isZero();

        ConsistencyCommitResponse docsResult =
                docsGroup.commits().get(0);

        assertThat(docsResult.sha())
                .isEqualTo("sha-docs");

        assertThat(docsResult.commitType())
                .isEqualTo("docs");

        assertThat(docsResult.scope())
                .isEqualTo("assignments");
        assertThat(docsResult.consistent()).isTrue();
        assertThat(docsResult.issues()).isEmpty();

        ConsistencyGroupResponse studyGroup =
                findGroup(
                        response,
                        "study(java):"
                );

        assertThat(studyGroup.pathPattern())
                .isEqualTo("assignments/**");

        assertThat(studyGroup.category())
                .isEqualTo("assignments");

        assertThat(studyGroup.scope())
                .isEqualTo("java");

        assertThat(studyGroup.commitCount()).isEqualTo(1);
        assertThat(studyGroup.consistentCount()).isEqualTo(1);
        assertThat(studyGroup.inconsistentCount()).isZero();

        ConsistencyCommitResponse studyResult =
                studyGroup.commits().get(0);

        assertThat(studyResult.sha())
                .isEqualTo("sha-study");

        assertThat(studyResult.commitType())
                .isEqualTo("study");

        assertThat(studyResult.scope())
                .isEqualTo("java");

        assertThat(studyResult.consistent()).isTrue();
        assertThat(studyResult.issues()).isEmpty();

        ConsistencyGroupResponse unmatchedGroup =
                response.groups()
                        .stream()
                        .filter(group ->
                                group.expectedPattern() == null
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(unmatchedGroup.pathPattern())
                .isEqualTo("assignments/**");

        assertThat(unmatchedGroup.category())
                .isEqualTo("assignments");

        assertThat(unmatchedGroup.scope()).isNull();
        assertThat(unmatchedGroup.commitCount()).isEqualTo(1);
        assertThat(unmatchedGroup.consistentCount()).isZero();
        assertThat(unmatchedGroup.inconsistentCount()).isEqualTo(1);

        ConsistencyCommitResponse unmatchedResult =
                unmatchedGroup.commits().get(0);

        assertThat(unmatchedResult.sha())
                .isEqualTo("sha-unmatched");

        assertThat(unmatchedResult.commitType()).isNull();
        assertThat(unmatchedResult.scope()).isNull();
        assertThat(unmatchedResult.consistent()).isFalse();

        assertThat(unmatchedResult.issues())
                .containsExactly(
                        "적용 가능한 커밋 규칙이 없습니다."
                );

        verify(
                gitHubCommitService,
                never()
        ).getCommitDetail(
                USER_ID,
                CONNECTED_REPOSITORY_ID,
                "sha-merge"
        );
    }

    @Test
    @DisplayName("같은 경로와 같은 커밋 규칙을 사용하는 커밋은 하나의 그룹으로 묶는다")
    void analyzeGroupsCommitsWithSameConventionRuleTogether() {
        ClassificationRule classificationRule =
                new ClassificationRule(
                        connectedRepository,
                        "lectures/**",
                        "lectures",
                        null,
                        100,
                        true
                );

        ConventionRule conventionRule =
                new ConventionRule(
                        connectedRepository,
                        "docs(lectures):",
                        "docs",
                        null,
                        "lectures",
                        100,
                        true
                );

        ClassificationMatchResult classificationMatch =
                new ClassificationMatchResult(
                        1L,
                        "lectures/**",
                        "lectures",
                        null,
                        100
                );

        ConventionMatchResult conventionMatch =
                new ConventionMatchResult(
                        2L,
                        "docs(lectures):",
                        "docs",
                        null,
                        "lectures",
                        100
                );

        GitHubCommitResponse firstCommit =
                createCommit(
                        "sha-first",
                        "docs(lectures): 2026-07-29 Security 정리",
                        "2026-07-29T10:00:00Z"
                );

        GitHubCommitResponse secondCommit =
                createCommit(
                        "sha-second",
                        "docs(lectures): 2026-07-30 JWT 정리",
                        "2026-07-30T10:00:00Z"
                );

        stubOwnedRepository();

        when(
                classificationRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        )
        ).thenReturn(List.of(classificationRule));

        when(
                conventionRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        )
        ).thenReturn(List.of(conventionRule));

        when(
                gitHubCommitService.getCommits(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                )
        ).thenReturn(
                List.of(
                        firstCommit,
                        secondCommit
                )
        );

        when(
                gitHubCommitService.getCommitDetail(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        "sha-first"
                )
        ).thenReturn(
                createDetail(
                        "sha-first",
                        "lectures/security.md"
                )
        );

        when(
                gitHubCommitService.getCommitDetail(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        "sha-second"
                )
        ).thenReturn(
                createDetail(
                        "sha-second",
                        "lectures/jwt.md"
                )
        );

        when(
                repositoryRuleMatcher.matchClassificationRule(
                        eq("lectures/security.md"),
                        anyList()
                )
        ).thenReturn(Optional.of(classificationMatch));

        when(
                repositoryRuleMatcher.matchClassificationRule(
                        eq("lectures/jwt.md"),
                        anyList()
                )
        ).thenReturn(Optional.of(classificationMatch));

        when(
                repositoryRuleMatcher.matchConventionRule(
                        eq(firstCommit.message()),
                        anyList()
                )
        ).thenReturn(Optional.of(conventionMatch));

        when(
                repositoryRuleMatcher.matchConventionRule(
                        eq(secondCommit.message()),
                        anyList()
                )
        ).thenReturn(Optional.of(conventionMatch));

        CommitConsistencyResponse response =
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                );

        assertThat(response.commitCount()).isEqualTo(2);
        assertThat(response.consistentCount()).isEqualTo(2);
        assertThat(response.inconsistentCount()).isZero();
        assertThat(response.groups()).hasSize(1);

        ConsistencyGroupResponse group =
                response.groups().get(0);

        assertThat(group.pathPattern())
                .isEqualTo("lectures/**");

        assertThat(group.category())
                .isEqualTo("lectures");

        assertThat(group.expectedPattern())
                .isEqualTo("docs(lectures):");

        assertThat(group.commitCount()).isEqualTo(2);
        assertThat(group.consistentCount()).isEqualTo(2);
        assertThat(group.inconsistentCount()).isZero();

        assertThat(group.commits())
                .extracting(
                        ConsistencyCommitResponse::sha
                )
                .containsExactly(
                        "sha-first",
                        "sha-second"
                );

        assertThat(group.commits())
                .allMatch(
                        ConsistencyCommitResponse::consistent
                );
    }

    @Test
    @DisplayName("경로 규칙이 없으면 미분류 경로에서도 커밋 규칙별로 그룹화한다")
    void analyzeGroupsUnclassifiedPathByConventionRule() {
        ConventionRule conventionRule =
                new ConventionRule(
                        connectedRepository,
                        "chore(project):",
                        "chore",
                        "project",
                        "project",
                        100,
                        true
                );

        ConventionMatchResult conventionMatch =
                new ConventionMatchResult(
                        13L,
                        "chore(project):",
                        "chore",
                        "project",
                        "project",
                        100
                );

        GitHubCommitResponse commit =
                createCommit(
                        "sha-project",
                        "chore(project): 프로젝트 설정 수정",
                        "2026-07-30T10:00:00Z"
                );

        stubOwnedRepository();

        when(
                classificationRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        )
        ).thenReturn(List.of());

        when(
                conventionRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        )
        ).thenReturn(List.of(conventionRule));

        when(
                gitHubCommitService.getCommits(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                )
        ).thenReturn(List.of(commit));

        when(
                gitHubCommitService.getCommitDetail(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        "sha-project"
                )
        ).thenReturn(
                createDetail(
                        "sha-project",
                        "build.gradle"
                )
        );

        when(
                repositoryRuleMatcher.matchClassificationRule(
                        eq("build.gradle"),
                        anyList()
                )
        ).thenReturn(Optional.empty());

        when(
                repositoryRuleMatcher.matchConventionRule(
                        eq(commit.message()),
                        anyList()
                )
        ).thenReturn(Optional.of(conventionMatch));

        CommitConsistencyResponse response =
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                );

        assertThat(response.commitCount()).isEqualTo(1);
        assertThat(response.consistentCount()).isEqualTo(1);
        assertThat(response.inconsistentCount()).isZero();
        assertThat(response.groups()).hasSize(1);

        ConsistencyGroupResponse group =
                response.groups().get(0);

        assertThat(group.pathPattern()).isNull();
        assertThat(group.category())
                .isEqualTo("unclassified");

        assertThat(group.scope())
                .isEqualTo("project");

        assertThat(group.expectedPattern())
                .isEqualTo("chore(project):");

        assertThat(group.consistentCount()).isEqualTo(1);
        assertThat(group.inconsistentCount()).isZero();

        ConsistencyCommitResponse commitResponse =
                group.commits().get(0);

        assertThat(commitResponse.commitType())
                .isEqualTo("chore");

        assertThat(commitResponse.scope())
                .isEqualTo("project");

        assertThat(commitResponse.category())
                .isEqualTo("unclassified");

        assertThat(commitResponse.consistent()).isTrue();
        assertThat(commitResponse.issues()).isEmpty();
    }

    @Test
    @DisplayName("분석 시작일이나 종료일이 없으면 예외가 발생한다")
    void analyzeThrowsExceptionWhenPeriodIsMissing() {
        assertThatThrownBy(() ->
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        null,
                        TO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "분석 시작일과 종료일은 필수입니다."
                );

        assertThatThrownBy(() ->
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "분석 시작일과 종료일은 필수입니다."
                );

        verify(
                userRepository,
                never()
        ).findById(USER_ID);
    }

    @Test
    @DisplayName("분석 시작일이 종료일보다 늦으면 예외가 발생한다")
    void analyzeThrowsExceptionWhenFromIsAfterTo() {
        LocalDate invalidFrom =
                LocalDate.of(2026, 8, 1);

        LocalDate invalidTo =
                LocalDate.of(2026, 7, 1);

        assertThatThrownBy(() ->
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        invalidFrom,
                        invalidTo
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "분석 시작일은 종료일보다 늦을 수 없습니다."
                );

        verify(
                userRepository,
                never()
        ).findById(USER_ID);

        verify(
                gitHubCommitService,
                never()
        ).getCommits(
                USER_ID,
                CONNECTED_REPOSITORY_ID,
                invalidFrom,
                invalidTo
        );
    }

    @Test
    @DisplayName("사용자 소유가 아닌 연결 저장소는 분석할 수 없다")
    void analyzeThrowsExceptionWhenRepositoryIsNotOwnedByUser() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(
                connectedRepositoryRepository.findByIdAndUser(
                        CONNECTED_REPOSITORY_ID,
                        user
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                commitConsistencyService.analyze(
                        USER_ID,
                        CONNECTED_REPOSITORY_ID,
                        FROM,
                        TO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "연결된 저장소를 찾을 수 없습니다."
                );

        verify(
                gitHubCommitService,
                never()
        ).getCommits(
                USER_ID,
                CONNECTED_REPOSITORY_ID,
                FROM,
                TO
        );
    }

    private void stubOwnedRepository() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(
                connectedRepositoryRepository.findByIdAndUser(
                        CONNECTED_REPOSITORY_ID,
                        user
                )
        ).thenReturn(Optional.of(connectedRepository));
    }

    private GitHubCommitResponse createCommit(
            String sha,
            String message,
            String committedAt
    ) {
        Instant instant = Instant.parse(committedAt);

        GitHubCommitResponse.GitUserInfo userInfo =
                new GitHubCommitResponse.GitUserInfo(
                        "tester",
                        "tester@example.com",
                        instant
                );

        GitHubCommitResponse.CommitInfo commitInfo =
                new GitHubCommitResponse.CommitInfo(
                        message,
                        userInfo,
                        userInfo
                );

        return new GitHubCommitResponse(
                sha,
                "https://github.com/example/repository/commit/"
                        + sha,
                commitInfo
        );
    }

    private GitHubCommitDetailResponse createDetail(
            String sha,
            String filename
    ) {
        GitHubCommitDetailResponse.ChangedFile file =
                new GitHubCommitDetailResponse.ChangedFile(
                        filename,
                        "modified",
                        10,
                        2,
                        12,
                        null
                );

        return new GitHubCommitDetailResponse(
                sha,
                "https://github.com/example/repository/commit/"
                        + sha,
                List.of(file)
        );
    }

    private ConsistencyGroupResponse findGroup(
            CommitConsistencyResponse response,
            String expectedPattern
    ) {
        return response.groups()
                .stream()
                .filter(group ->
                        expectedPattern.equals(
                                group.expectedPattern()
                        )
                )
                .findFirst()
                .orElseThrow();
    }
}