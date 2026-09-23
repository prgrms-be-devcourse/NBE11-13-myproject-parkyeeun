package com.repoary.backend.recordcheck.service;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.dto.GitHubContentResponse;
import com.repoary.backend.github.exception.GitHubErrorCode;
import com.repoary.backend.recordcheck.dto.RecordCheckItemResponse;
import com.repoary.backend.recordcheck.dto.RecordCheckResponse;
import com.repoary.backend.recordcheck.exception.RecordCheckErrorCode;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.exception.RepositoryErrorCode;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecordCheckServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime DAY_BOUNDARY = LocalTime.of(6, 0);
    private static final Long USER_ID = 1L;
    private static final Long CONNECTED_REPOSITORY_ID = 10L;
    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);

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
    void 월_디렉터리를_한_번_조회하고_날짜별_TIL은_조회하지_않는다() {
        LocalDate first = LocalDate.of(2026, 9, 9);
        LocalDate second = LocalDate.of(2026, 9, 10);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(kotlin): 2026-09-09 컬렉션", first),
                commit("study(kotlin): 2026-09-10 게시판", second)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of(second)));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of(
                tilEntry(second)
        )));

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).tilExists()).isFalse();
        assertThat(response.items().get(0).readmeEntryExists()).isFalse();
        assertThat(response.items().get(1).tilExists()).isTrue();
        assertThat(response.items().get(1).readmeEntryExists()).isTrue();
        verifyDirectoryOnce(SEPTEMBER);
        verifyNoPerFileTilLookup();
    }

    @Test
    void 월_디렉터리의_모든_TIL을_존재로_판정한다() {
        LocalDate first = LocalDate.of(2026, 9, 1);
        LocalDate second = LocalDate.of(2026, 9, 2);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", first),
                commit("study(java): 2026-09-02", second)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of(first, second)));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of(
                tilEntry(first),
                tilEntry(second)
        )));

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items())
                .allSatisfy(item -> {
                    assertThat(item.tilExists()).isTrue();
                    assertThat(item.readmeEntryExists()).isTrue();
                });
        verifyNoPerFileTilLookup();
    }

    @Test
    void 빈_월_디렉터리는_모든_TIL을_누락으로_판정한다() {
        LocalDate first = LocalDate.of(2026, 9, 1);
        LocalDate second = LocalDate.of(2026, 9, 2);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", first),
                commit("study(java): 2026-09-02", second)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of(first, second)));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of()));

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items())
                .allSatisfy(item -> assertThat(item.tilExists()).isFalse());
        verifyNoPerFileTilLookup();
    }

    @Test
    void 월_디렉터리_404는_모든_TIL을_누락으로_판정한다() {
        LocalDate date = LocalDate.of(2026, 9, 22);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-22", date)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.empty());

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.tilExists()).isFalse();
            assertThat(item.readmeEntryExists()).isFalse();
        });
        verifyNoPerFileTilLookup();
    }

    @Test
    void README가_없어도_월_디렉터리의_TIL은_존재로_판정한다() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-10", date)
        ));
        givenMonthlyReadme(SEPTEMBER, Optional.empty());
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of(
                tilEntry(date)
        )));

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.tilExists()).isTrue();
            assertThat(item.readmeEntryExists()).isFalse();
        });
    }

    @Test
    void 점검_대상_날짜가_없으면_README와_월_디렉터리를_조회하지_않는다() {
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of());

        assertThat(check(SEPTEMBER).items()).isEmpty();

        verify(gitHubApiClient, never()).getContent(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
        verify(gitHubApiClient, never()).getDirectoryContents(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void 다른_날짜와_유사한_파일명을_TIL로_오인하지_않는다() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", date)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of(
                entry(
                        "2026-09-01-copy.md",
                        "til/2026-09/2026-09-01-copy.md",
                        "file"
                ),
                entry(
                        "2026-09-11.md",
                        "til/2026-09/2026-09-11.md",
                        "file"
                )
        )));

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items().get(0).tilExists()).isFalse();
        verifyNoPerFileTilLookup();
    }

    @Test
    void 디렉터리_항목이_1000개이면_날짜별_조회로_fallback한다() {
        LocalDate exists = LocalDate.of(2026, 9, 1);
        LocalDate missing = LocalDate.of(2026, 9, 2);
        List<GitHubContentResponse> entries = IntStream.range(0, 1_000)
                .mapToObj(index -> entry(
                        "other-" + index + ".md",
                        "til/2026-09/other-" + index + ".md",
                        "file"
                ))
                .toList();
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", exists),
                commit("study(java): 2026-09-02", missing)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(entries));
        givenTilFile(exists, Optional.of(tilEntry(exists)));
        givenTilFile(missing, Optional.empty());

        RecordCheckResponse response = check(SEPTEMBER);

        assertThat(response.items().get(0).tilExists()).isTrue();
        assertThat(response.items().get(1).tilExists()).isFalse();
        verifyPerFileTilLookups(2);
    }

    @Test
    void 대상_항목의_타입이_모호하면_날짜별_조회로_fallback한다() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", date)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of(
                entry(
                        "2026-09-01.md",
                        "til/2026-09/2026-09-01.md",
                        "symlink"
                )
        )));
        givenTilFile(date, Optional.of(tilEntry(date)));

        assertThat(check(SEPTEMBER).items().get(0).tilExists()).isTrue();
        verifyPerFileTilLookups(1);
    }

    @Test
    void null_디렉터리_항목은_빈_목록으로_오인하지_않고_fallback한다() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        List<GitHubContentResponse> entries = new ArrayList<>();
        entries.add(null);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", date)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(entries));
        givenTilFile(date, Optional.empty());

        assertThat(check(SEPTEMBER).items().get(0).tilExists()).isFalse();
        verifyPerFileTilLookups(1);
    }

    @Test
    void 대상_파일의_name과_path가_불일치하면_fallback한다() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-01", date)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of(
                entry(
                        "2026-09-01.md",
                        "til/other/2026-09-01.md",
                        "file"
                )
        )));
        givenTilFile(date, Optional.empty());

        check(SEPTEMBER);

        verifyPerFileTilLookups(1);
    }

    @Test
    void 같은_날짜에_커밋이_여러_개여도_한_번만_판정한다() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(kotlin): 2026-09-10 첫 번째", date),
                commit("study(kotlin): 2026-09-10 두 번째", date)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of()));

        assertThat(check(SEPTEMBER).items()).hasSize(1);
    }

    @Test
    void TIL_작성_커밋만_있으면_점검_대상에서_제외한다() {
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit(
                        "docs(til): 2026-09-10 TIL 작성",
                        LocalDate.of(2026, 9, 10)
                )
        ));

        assertThat(check(SEPTEMBER).items()).isEmpty();
        verify(gitHubApiClient, never()).getDirectoryContents(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void README_프로젝트_목록_관리_커밋은_7월_26일_학습일을_만들지_않는다() {
        YearMonth july = YearMonth.of(2026, 7);
        GitHubCommitResponse readmeCommit = commit(
                "docs(readme): 프로젝트 목록 추가",
                LocalDate.of(2026, 7, 27)
                        .atTime(1, 4, 43)
                        .atZone(KST)
                        .toInstant()
        );
        givenRepositoryContext();
        givenMonthlyCommits(july, List.of(readmeCommit));

        assertThat(check(july).items()).isEmpty();
        verify(gitHubApiClient, never()).getContent(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
        verify(gitHubApiClient, never()).getDirectoryContents(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void README_관리_커밋과_같은_날의_study_커밋은_학습일로_유지한다() {
        YearMonth july = YearMonth.of(2026, 7);
        LocalDate learningDate = LocalDate.of(2026, 7, 27);
        givenRepositoryContext();
        givenMonthlyCommits(july, List.of(
                commit(
                        "docs(readme): 프로젝트 목록 추가",
                        learningDate.atTime(1, 4, 43)
                                .atZone(KST)
                                .toInstant()
                ),
                commit(
                        "study(springboot): 2026-07-27 HTTP Basic 인증 실습",
                        learningDate
                )
        ));
        givenMonthlyReadme(july, readme(Set.of()));
        givenMonthlyDirectory(july, Optional.of(List.of()));

        assertThat(check(july).items())
                .extracting(RecordCheckItemResponse::date)
                .containsExactly(learningDate);
    }

    @Test
    void assignments_lectures_practice_문서_커밋은_학습일로_포함한다() {
        LocalDate assignmentsDate = LocalDate.of(2026, 9, 8);
        LocalDate lecturesDate = LocalDate.of(2026, 9, 21);
        LocalDate practiceDate = LocalDate.of(2026, 9, 22);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit(
                        "docs(assignments): Kotlin 회원 관리 과제 문서 정리",
                        assignmentsDate
                ),
                commit(
                        "docs(lectures): 2026-09-21 네트워크와 Docker 기초 내용 정리",
                        lecturesDate
                ),
                commit(
                        "docs(practice): MSA 아키텍처 시각화 자료 추가",
                        practiceDate
                )
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of()));

        assertThat(check(SEPTEMBER).items())
                .extracting(RecordCheckItemResponse::date)
                .containsExactly(
                        assignmentsDate,
                        lecturesDate,
                        practiceDate
                );
    }

    @Test
    void refactor_style_chore_커밋은_학습일로_포함한다() {
        LocalDate refactorDate = LocalDate.of(2026, 9, 1);
        LocalDate styleDate = LocalDate.of(2026, 9, 2);
        LocalDate choreDate = LocalDate.of(2026, 9, 3);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("refactor(springboot): DTO 구조 개선", refactorDate),
                commit("style(springboot): 화면 스타일 개선", styleDate),
                commit("chore(project): 실습 프로젝트 초기 설정", choreDate)
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of()));

        assertThat(check(SEPTEMBER).items())
                .extracting(RecordCheckItemResponse::date)
                .containsExactly(refactorDate, styleDate, choreDate);
    }

    @Test
    void 새벽_6시_이전_커밋은_전날_학습으로_처리한다() {
        LocalDate learningDate = LocalDate.of(2026, 9, 9);
        GitHubCommitResponse commit = commit(
                "study(kotlin): 컬렉션",
                LocalDate.of(2026, 9, 10)
                        .atTime(2, 30)
                        .atZone(KST)
                        .toInstant()
        );
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(commit));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of()));

        assertThat(check(SEPTEMBER).items().get(0).date())
                .isEqualTo(learningDate);
    }

    @Test
    void 오전_5시_59분은_전날이고_6시는_당일로_처리한다() {
        LocalDate septemberNinth = LocalDate.of(2026, 9, 9);
        LocalDate septemberTenth = LocalDate.of(2026, 9, 10);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit(
                        "study(kotlin): 날짜 경계 이전",
                        septemberTenth.atTime(5, 59)
                                .atZone(KST)
                                .toInstant()
                ),
                commit(
                        "study(kotlin): 날짜 경계",
                        septemberTenth.atTime(6, 0)
                                .atZone(KST)
                                .toInstant()
                )
        ));
        givenMonthlyReadme(SEPTEMBER, readme(Set.of()));
        givenMonthlyDirectory(SEPTEMBER, Optional.of(List.of()));

        assertThat(check(SEPTEMBER).items())
                .extracting(RecordCheckItemResponse::date)
                .containsExactly(septemberNinth, septemberTenth);
    }

    @Test
    void 실제_커밋_시각보다_커밋_메시지의_학습_날짜를_우선한다() {
        YearMonth august = YearMonth.of(2026, 8);
        LocalDate learningDate = LocalDate.of(2026, 8, 31);
        GitHubCommitResponse commit = commit(
                "study(springboot): 2026-08-31 MSA",
                LocalDate.of(2026, 9, 1)
                        .atTime(15, 0)
                        .atZone(KST)
                        .toInstant()
        );
        givenRepositoryContext();
        givenMonthlyCommits(august, List.of(commit));
        givenMonthlyReadme(august, readme(Set.of()));
        givenMonthlyDirectory(august, Optional.of(List.of()));

        assertThat(check(august).items().get(0).date())
                .isEqualTo(learningDate);
    }

    @Test
    void README_인코딩이_base64가_아니면_잘못된_GitHub_응답이다() {
        assertInvalidReadme(new GitHubContentResponse(
                "README.md",
                "til/2026-09/README.md",
                "file",
                "content",
                "utf-8"
        ));
    }

    @Test
    void README의_base64_내용이_잘못되면_잘못된_GitHub_응답이다() {
        assertInvalidReadme(new GitHubContentResponse(
                "README.md",
                "til/2026-09/README.md",
                "file",
                "A",
                "base64"
        ));
    }

    @Test
    void 다른_사용자의_저장소는_점검할_수_없다() {
        given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(user));
        given(connectedRepositoryRepository.findByIdAndUser(
                CONNECTED_REPOSITORY_ID,
                user
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> check(SEPTEMBER))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        RepositoryErrorCode.CONNECTED_REPOSITORY_NOT_FOUND
                                )
                );
    }

    @Test
    void 점검할_월이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> check(null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(RecordCheckErrorCode.MONTH_REQUIRED)
                );
    }

    private RecordCheckResponse check(YearMonth month) {
        return recordCheckService.check(
                USER_ID,
                CONNECTED_REPOSITORY_ID,
                month
        );
    }

    private void givenRepositoryContext() {
        given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(user));
        given(connectedRepositoryRepository.findByIdAndUser(
                CONNECTED_REPOSITORY_ID,
                user
        )).willReturn(Optional.of(repository));
        given(user.getGithubAccessToken()).willReturn("github-access-token");
        given(repository.getFullName())
                .willReturn("dPdms21/programmers-devcourse-be11");
        given(repository.getDefaultBranch()).willReturn("main");
    }

    private void givenMonthlyCommits(
            YearMonth month,
            List<GitHubCommitResponse> commits
    ) {
        Instant since = month.atDay(1)
                .minusDays(7)
                .atTime(DAY_BOUNDARY)
                .atZone(KST)
                .toInstant();
        Instant until = month.plusMonths(1)
                .atDay(1)
                .plusDays(7)
                .atTime(DAY_BOUNDARY)
                .atZone(KST)
                .toInstant();
        given(gitHubApiClient.getCommits(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main",
                since,
                until
        )).willReturn(commits);
    }

    private void givenMonthlyReadme(
            YearMonth month,
            Optional<GitHubContentResponse> response
    ) {
        given(gitHubApiClient.getContent(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main",
                "til/" + month + "/README.md"
        )).willReturn(response);
    }

    private void givenMonthlyDirectory(
            YearMonth month,
            Optional<List<GitHubContentResponse>> response
    ) {
        given(gitHubApiClient.getDirectoryContents(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main",
                "til/" + month
        )).willReturn(response);
    }

    private void givenTilFile(
            LocalDate date,
            Optional<GitHubContentResponse> response
    ) {
        given(gitHubApiClient.getContent(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main",
                "til/" + YearMonth.from(date) + "/" + date + ".md"
        )).willReturn(response);
    }

    private Optional<GitHubContentResponse> readme(Set<LocalDate> dates) {
        String content = dates.stream()
                .sorted()
                .map(date -> "[" + date + "](./" + date + ".md)")
                .reduce("", (left, right) -> left + right + "\n");
        return Optional.of(new GitHubContentResponse(
                "README.md",
                "til/2026-09/README.md",
                "file",
                Base64.getEncoder().encodeToString(
                        content.getBytes(StandardCharsets.UTF_8)
                ),
                "base64"
        ));
    }

    private GitHubContentResponse tilEntry(LocalDate date) {
        return entry(
                date + ".md",
                "til/" + YearMonth.from(date) + "/" + date + ".md",
                "file"
        );
    }

    private GitHubContentResponse entry(
            String name,
            String path,
            String type
    ) {
        return new GitHubContentResponse(name, path, type, null, null);
    }

    private GitHubCommitResponse commit(
            String message,
            LocalDate date
    ) {
        return commit(
                message,
                date.atTime(15, 0).atZone(KST).toInstant()
        );
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
        return new GitHubCommitResponse(
                "commit-sha",
                "https://github.com/example/commit",
                new GitHubCommitResponse.CommitInfo(
                        message,
                        committer,
                        committer
                )
        );
    }

    private void verifyDirectoryOnce(YearMonth month) {
        verify(gitHubApiClient, times(1)).getDirectoryContents(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main",
                "til/" + month
        );
    }

    private void verifyNoPerFileTilLookup() {
        verify(gitHubApiClient, never()).getContent(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                argThat(path -> path != null
                        && path.matches("til/\\d{4}-\\d{2}/\\d{4}-\\d{2}-\\d{2}\\.md"))
        );
    }

    private void verifyPerFileTilLookups(int count) {
        verify(gitHubApiClient, times(count)).getContent(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                argThat(path -> path != null
                        && path.matches("til/\\d{4}-\\d{2}/\\d{4}-\\d{2}-\\d{2}\\.md"))
        );
    }

    private void assertInvalidReadme(GitHubContentResponse content) {
        LocalDate date = LocalDate.of(2026, 9, 10);
        givenRepositoryContext();
        givenMonthlyCommits(SEPTEMBER, List.of(
                commit("study(java): 2026-09-10", date)
        ));
        givenMonthlyReadme(SEPTEMBER, Optional.of(content));

        assertThatThrownBy(() -> check(SEPTEMBER))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.INVALID_RESPONSE)
                );
        verify(gitHubApiClient, never()).getDirectoryContents(
                anyString(), anyString(), anyString(), anyString(), anyString()
        );
    }
}
