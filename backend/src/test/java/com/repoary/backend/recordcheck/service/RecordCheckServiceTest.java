package com.repoary.backend.recordcheck.service;

import com.repoary.backend.common.exception.NotFoundException;
import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.dto.GitHubContentResponse;
import com.repoary.backend.recordcheck.dto.RecordCheckResponse;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class RecordCheckServiceTest {

    private static final ZoneId KST =
            ZoneId.of("Asia/Seoul");

    private static final LocalTime DAY_BOUNDARY =
            LocalTime.of(6, 0);

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectedRepositoryRepository connectedRepositoryRepository;

    @Mock
    private GitHubApiClient gitHubApiClient;

    @InjectMocks
    private RecordCheckService recordCheckService;

    private User user;
    private ConnectedRepository repository;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        repository = mock(ConnectedRepository.class);
    }

    @Test
    void 실제_GitHub_커밋이_있는_날짜의_TIL과_README_반영_여부를_점검한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        LocalDate september9 =
                LocalDate.of(2026, 9, 9);

        LocalDate september10 =
                LocalDate.of(2026, 9, 10);

        GitHubCommitResponse commit9 =
                commit(
                        "study(kotlin): 2026-09-09 컬렉션 실습",
                        september9
                                .atTime(15, 0)
                                .atZone(KST)
                                .toInstant()
                );

        GitHubCommitResponse commit10 =
                commit(
                        "study(kotlin): 2026-09-10 게시판 실습",
                        september10
                                .atTime(15, 0)
                                .atZone(KST)
                                .toInstant()
                );

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of(commit9, commit10)
        );

        String readme = """
                ## Week 2 (2026-09-07 ~ 2026-09-13)

                | Date | Summary |
                | --- | --- |
                | [2026-09-10](./2026-09-10.md) | Kotlin 게시판 실습 |
                """;

        givenMonthlyReadme(
                month,
                Optional.of(
                        new GitHubContentResponse(
                                "README.md",
                                "til/2026-09/README.md",
                                "file",
                                encode(readme),
                                "base64"
                        )
                )
        );

        givenTilFile(
                september9,
                Optional.empty()
        );

        givenTilFile(
                september10,
                Optional.of(
                        new GitHubContentResponse(
                                "2026-09-10.md",
                                "til/2026-09/2026-09-10.md",
                                "file",
                                null,
                                null
                        )
                )
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.month())
                .isEqualTo(month);

        assertThat(response.items())
                .hasSize(2);

        assertThat(response.items().get(0).date())
                .isEqualTo(september9);

        assertThat(response.items().get(0).tilExists())
                .isFalse();

        assertThat(response.items().get(0).readmeEntryExists())
                .isFalse();

        assertThat(response.items().get(1).date())
                .isEqualTo(september10);

        assertThat(response.items().get(1).tilExists())
                .isTrue();

        assertThat(response.items().get(1).readmeEntryExists())
                .isTrue();
    }

    @Test
    void 같은_날짜에_커밋이_여러_개여도_한_번만_점검한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        LocalDate targetDate =
                LocalDate.of(2026, 9, 10);

        GitHubCommitResponse first =
                commit(
                        "study(kotlin): 2026-09-10 게시판 실습",
                        targetDate
                                .atTime(10, 0)
                                .atZone(KST)
                                .toInstant()
                );

        GitHubCommitResponse second =
                commit(
                        "study(kotlin): 2026-09-10 컬렉션 실습",
                        targetDate
                                .atTime(18, 0)
                                .atZone(KST)
                                .toInstant()
                );

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of(first, second)
        );

        givenMonthlyReadme(
                month,
                Optional.empty()
        );

        givenTilFile(
                targetDate,
                Optional.empty()
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.items())
                .hasSize(1);

        assertThat(response.items().get(0).date())
                .isEqualTo(targetDate);
    }

    @Test
    void TIL_작성_커밋만_있는_날짜는_점검_대상에서_제외한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        GitHubCommitResponse tilCommit =
                commit(
                        "docs(til): 2026-09-10 TIL 작성",
                        LocalDate.of(2026, 9, 10)
                                .atTime(23, 0)
                                .atZone(KST)
                                .toInstant()
                );

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of(tilCommit)
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.items())
                .isEmpty();

        verify(
                gitHubApiClient,
                never()
        ).getContent(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void 새벽_6시_이전_커밋은_전날_학습으로_처리한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        LocalDate learningDate =
                LocalDate.of(2026, 9, 9);

        GitHubCommitResponse commit =
                commit(
                        "study(kotlin): 컬렉션 실습",
                        LocalDate.of(2026, 9, 10)
                                .atTime(2, 30)
                                .atZone(KST)
                                .toInstant()
                );

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of(commit)
        );

        givenMonthlyReadme(
                month,
                Optional.empty()
        );

        givenTilFile(
                learningDate,
                Optional.empty()
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.items())
                .hasSize(1);

        assertThat(response.items().get(0).date())
                .isEqualTo(learningDate);
    }

    @Test
    void README_파일이_없으면_README_행을_누락으로_처리한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        LocalDate targetDate =
                LocalDate.of(2026, 9, 10);

        GitHubCommitResponse commit =
                commit(
                        "study(kotlin): 2026-09-10 게시판 실습",
                        targetDate
                                .atTime(15, 0)
                                .atZone(KST)
                                .toInstant()
                );

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of(commit)
        );

        givenMonthlyReadme(
                month,
                Optional.empty()
        );

        givenTilFile(
                targetDate,
                Optional.of(
                        new GitHubContentResponse(
                                "2026-09-10.md",
                                "til/2026-09/2026-09-10.md",
                                "file",
                                null,
                                null
                        )
                )
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.items())
                .hasSize(1);

        assertThat(response.items().get(0).tilExists())
                .isTrue();

        assertThat(response.items().get(0).readmeEntryExists())
                .isFalse();
    }

    @Test
    void 학습_커밋이_없으면_빈_목록을_반환한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 9);

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of()
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.items())
                .isEmpty();

        verify(
                gitHubApiClient,
                never()
        ).getContent(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void 다른_사용자의_저장소는_점검할_수_없다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;

        given(userRepository.findById(userId))
                .willReturn(
                        Optional.of(user)
                );

        given(
                connectedRepositoryRepository.findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
        ).willReturn(
                Optional.empty()
        );

        assertThatThrownBy(
                () -> recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        YearMonth.of(2026, 9)
                )
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage(
                        "연결된 저장소를 찾을 수 없습니다."
                );
    }

    @Test
    void 점검할_월이_없으면_예외가_발생한다() {
        assertThatThrownBy(
                () -> recordCheckService.check(
                        1L,
                        10L,
                        null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "점검할 월은 필수입니다."
                );
    }

    @Test
    void 실제_커밋_시각보다_커밋_메시지의_학습_날짜를_우선한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        YearMonth month = YearMonth.of(2026, 8);

        LocalDate learningDate =
                LocalDate.of(2026, 8, 31);

        GitHubCommitResponse commit =
                commit(
                        "study(springboot): 2026-08-31 MSA 사용자 정보와 게시판 조회 실습",
                        LocalDate.of(2026, 9, 1)
                                .atTime(15, 0)
                                .atZone(KST)
                                .toInstant()
                );

        givenRepositoryContext(
                userId,
                connectedRepositoryId
        );

        givenMonthlyCommits(
                month,
                List.of(commit)
        );

        givenMonthlyReadme(
                month,
                Optional.empty()
        );

        givenTilFile(
                learningDate,
                Optional.empty()
        );

        RecordCheckResponse response =
                recordCheckService.check(
                        userId,
                        connectedRepositoryId,
                        month
                );

        assertThat(response.items())
                .hasSize(1);

        assertThat(response.items().get(0).date())
                .isEqualTo(learningDate);
    }

    private void givenRepositoryContext(
            Long userId,
            Long connectedRepositoryId
    ) {
        given(userRepository.findById(userId))
                .willReturn(
                        Optional.of(user)
                );

        given(
                connectedRepositoryRepository.findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
        ).willReturn(
                Optional.of(repository)
        );

        given(user.getGithubAccessToken())
                .willReturn(
                        "github-access-token"
                );

        given(repository.getFullName())
                .willReturn(
                        "dPdms21/programmers-devcourse-be11"
                );

        given(repository.getDefaultBranch())
                .willReturn(
                        "main"
                );
    }

    private void givenMonthlyCommits(
            YearMonth month,
            List<GitHubCommitResponse> commits
    ) {
        Instant since =
                month.atDay(1)
                        .minusDays(7)
                        .atTime(DAY_BOUNDARY)
                        .atZone(KST)
                        .toInstant();

        Instant until =
                month.plusMonths(1)
                        .atDay(1)
                        .plusDays(7)
                        .atTime(DAY_BOUNDARY)
                        .atZone(KST)
                        .toInstant();

        given(
                gitHubApiClient.getCommits(
                        "github-access-token",
                        "dPdms21",
                        "programmers-devcourse-be11",
                        "main",
                        since,
                        until
                )
        ).willReturn(commits);
    }

    private void givenMonthlyReadme(
            YearMonth month,
            Optional<GitHubContentResponse> response
    ) {
        String path =
                "til/" + month + "/README.md";

        given(
                gitHubApiClient.getContent(
                        "github-access-token",
                        "dPdms21",
                        "programmers-devcourse-be11",
                        "main",
                        path
                )
        ).willReturn(response);
    }

    private void givenTilFile(
            LocalDate targetDate,
            Optional<GitHubContentResponse> response
    ) {
        String path =
                "til/"
                        + YearMonth.from(targetDate)
                        + "/"
                        + targetDate
                        + ".md";

        given(
                gitHubApiClient.getContent(
                        "github-access-token",
                        "dPdms21",
                        "programmers-devcourse-be11",
                        "main",
                        path
                )
        ).willReturn(response);
    }

    private GitHubCommitResponse commit(
            String message,
            Instant committedAt
    ) {
        GitHubCommitResponse.GitUserInfo committer =
                new GitHubCommitResponse.GitUserInfo(
                        "tester",
                        "tester@example.com",
                        committedAt
                );

        GitHubCommitResponse.CommitInfo commitInfo =
                new GitHubCommitResponse.CommitInfo(
                        message,
                        committer,
                        committer
                );

        return new GitHubCommitResponse(
                "commit-sha",
                "https://github.com/example/commit",
                commitInfo
        );
    }

    private String encode(
            String content
    ) {
        return Base64.getEncoder()
                .encodeToString(
                        content.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }
}