package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.analysis.repository.AnalysisJobRepository;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalysisJobServiceTest {

    private AnalysisJobRepository analysisJobRepository;
    private CommitAnalysisService commitAnalysisService;
    private StoredAnalysisResultMapper storedAnalysisResultMapper;
    private UserRepository userRepository;
    private ConnectedRepositoryRepository connectedRepositoryRepository;
    private JsonMapper jsonMapper;

    private AnalysisJobService analysisJobService;

    private User user;
    private ConnectedRepository connectedRepository;

    @BeforeEach
    void setUp() {
        analysisJobRepository = mock(AnalysisJobRepository.class);
        commitAnalysisService = mock(CommitAnalysisService.class);
        storedAnalysisResultMapper = mock(StoredAnalysisResultMapper.class);
        userRepository = mock(UserRepository.class);
        connectedRepositoryRepository =
                mock(ConnectedRepositoryRepository.class);
        jsonMapper = mock(JsonMapper.class);

        analysisJobService = new AnalysisJobService(
                analysisJobRepository,
                commitAnalysisService,
                storedAnalysisResultMapper,
                userRepository,
                connectedRepositoryRepository,
                jsonMapper
        );

        user = mock(User.class);
        connectedRepository = mock(ConnectedRepository.class);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(
                connectedRepositoryRepository.findByIdAndUser(
                        11L,
                        user
                )
        ).thenReturn(Optional.of(connectedRepository));

        /*
         * 실제 DB 저장처럼 최초 저장 시 ID가 생성됐다고 가정한다.
         */
        when(analysisJobRepository.saveAndFlush(any(AnalysisJob.class)))
                .thenAnswer(invocation -> {
                    AnalysisJob analysisJob = invocation.getArgument(0);

                    if (analysisJob.getId() == null) {
                        ReflectionTestUtils.setField(
                                analysisJob,
                                "id",
                                1L
                        );
                    }

                    return analysisJob;
                });
    }

    @Test
    @DisplayName("분석에 성공하면 결과를 저장하고 COMPLETED 상태가 된다")
    void completeAnalysisJob() {
        LocalDate targetDate = LocalDate.of(2026, 7, 29);

        StoredAnalysisResult storedResult =
                new StoredAnalysisResult(
                        targetDate,
                        1,
                        List.of()
                );

        JsonNode resultJsonNode = mock(JsonNode.class);

        when(
                commitAnalysisService.analyzeCommits(
                        1L,
                        11L,
                        targetDate
                )
        ).thenReturn(List.of());

        when(
                storedAnalysisResultMapper.map(
                        targetDate,
                        List.of()
                )
        ).thenReturn(storedResult);

        when(jsonMapper.valueToTree(storedResult))
                .thenReturn(resultJsonNode);

        when(resultJsonNode.toString())
                .thenReturn("""
                        {
                          "targetDate": "2026-07-29",
                          "commitCount": 1,
                          "commits": []
                        }
                        """);

        AnalysisJob result = analysisJobService.execute(
                1L,
                11L,
                targetDate
        );

        assertThat(result.getStatus())
                .isEqualTo(AnalysisJobStatus.COMPLETED);

        assertThat(result.getResult())
                .contains("\"targetDate\": \"2026-07-29\"");

        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();

        verify(commitAnalysisService).analyzeCommits(
                1L,
                11L,
                targetDate
        );

        verify(analysisJobRepository, times(3))
                .saveAndFlush(any(AnalysisJob.class));
    }

    @Test
    @DisplayName("커밋이 없는 날짜도 빈 결과를 저장하고 COMPLETED 상태가 된다")
    void completeAnalysisJobWithoutCommits() {
        LocalDate targetDate = LocalDate.of(2026, 7, 28);

        StoredAnalysisResult emptyResult =
                new StoredAnalysisResult(
                        targetDate,
                        0,
                        List.of()
                );

        JsonNode resultJsonNode = mock(JsonNode.class);

        when(
                commitAnalysisService.analyzeCommits(
                        1L,
                        11L,
                        targetDate
                )
        ).thenReturn(List.of());

        when(
                storedAnalysisResultMapper.map(
                        targetDate,
                        List.of()
                )
        ).thenReturn(emptyResult);

        when(jsonMapper.valueToTree(emptyResult))
                .thenReturn(resultJsonNode);

        when(resultJsonNode.toString())
                .thenReturn("""
                        {
                          "targetDate": "2026-07-28",
                          "commitCount": 0,
                          "commits": []
                        }
                        """);

        AnalysisJob result = analysisJobService.execute(
                1L,
                11L,
                targetDate
        );

        assertThat(result.getStatus())
                .isEqualTo(AnalysisJobStatus.COMPLETED);

        assertThat(result.getResult())
                .contains("\"commitCount\": 0");

        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("분석 중 오류가 발생하면 FAILED 상태로 저장하고 예외를 다시 던진다")
    void failAnalysisJob() {
        LocalDate targetDate = LocalDate.of(2026, 7, 29);

        when(
                commitAnalysisService.analyzeCommits(
                        1L,
                        11L,
                        targetDate
                )
        ).thenThrow(
                new IllegalStateException(
                        "GitHub API 호출에 실패했습니다."
                )
        );

        when(analysisJobRepository.findById(1L))
                .thenAnswer(invocation -> {
                    AnalysisJob savedJob =
                            verifyAndGetLatestSavedJob();

                    return Optional.of(savedJob);
                });

        assertThatThrownBy(() ->
                analysisJobService.execute(
                        1L,
                        11L,
                        targetDate
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GitHub API 호출에 실패했습니다.");

        AnalysisJob failedJob = verifyAndGetLatestSavedJob();

        assertThat(failedJob.getStatus())
                .isEqualTo(AnalysisJobStatus.FAILED);

        assertThat(failedJob.getErrorMessage())
                .isEqualTo("GitHub API 호출에 실패했습니다.");

        assertThat(failedJob.getCompletedAt()).isNotNull();
        assertThat(failedJob.getResult()).isNull();
    }

    @Test
    @DisplayName("분석 날짜가 없으면 작업을 생성하지 않고 예외를 던진다")
    void rejectNullTargetDate() {
        assertThatThrownBy(() ->
                analysisJobService.execute(
                        1L,
                        11L,
                        null
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분석 날짜는 필수입니다.");

        verifyNoInteractions(
                userRepository,
                connectedRepositoryRepository,
                analysisJobRepository,
                commitAnalysisService
        );
    }

    @Test
    @DisplayName("사용자 소유가 아닌 저장소는 분석할 수 없다")
    void rejectUnownedRepository() {
        LocalDate targetDate = LocalDate.of(2026, 7, 29);

        when(
                connectedRepositoryRepository.findByIdAndUser(
                        11L,
                        user
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                analysisJobService.execute(
                        1L,
                        11L,
                        targetDate
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("연결된 저장소를 찾을 수 없습니다.");

        verify(analysisJobRepository, never())
                .saveAndFlush(any(AnalysisJob.class));
    }

    private AnalysisJob verifyAndGetLatestSavedJob() {
        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        AnalysisJob.class
                );

        verify(analysisJobRepository, atLeastOnce())
                .saveAndFlush(captor.capture());

        List<AnalysisJob> savedJobs =
                captor.getAllValues();

        return savedJobs.get(savedJobs.size() - 1);
    }
}