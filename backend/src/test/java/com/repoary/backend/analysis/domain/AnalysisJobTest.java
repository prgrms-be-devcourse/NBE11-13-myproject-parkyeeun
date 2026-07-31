package com.repoary.backend.analysis.domain;

import com.repoary.backend.repository.domain.ConnectedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AnalysisJobTest {

    private final ConnectedRepository connectedRepository =
            mock(ConnectedRepository.class);

    @Test
    @DisplayName("분석 작업을 생성하면 PENDING 상태가 된다")
    void createPendingJob() {
        AnalysisJob analysisJob = new AnalysisJob(
                connectedRepository,
                LocalDate.of(2026, 7, 29)
        );

        assertThat(analysisJob.getConnectedRepository())
                .isEqualTo(connectedRepository);
        assertThat(analysisJob.getTargetDate())
                .isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(analysisJob.getStatus())
                .isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(analysisJob.getResult()).isNull();
        assertThat(analysisJob.getErrorMessage()).isNull();
        assertThat(analysisJob.getStartedAt()).isNull();
        assertThat(analysisJob.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("PENDING 상태의 작업을 시작하면 RUNNING 상태가 된다")
    void startJob() {
        AnalysisJob analysisJob = createAnalysisJob();

        analysisJob.start();

        assertThat(analysisJob.getStatus())
                .isEqualTo(AnalysisJobStatus.RUNNING);
        assertThat(analysisJob.getStartedAt()).isNotNull();
        assertThat(analysisJob.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("RUNNING 상태의 작업을 완료하면 결과를 저장하고 COMPLETED 상태가 된다")
    void completeJob() {
        AnalysisJob analysisJob = createAnalysisJob();
        analysisJob.start();

        String result = """
                {
                  "targetDate": "2026-07-29",
                  "commitCount": 0,
                  "commits": []
                }
                """;

        analysisJob.complete(result);

        assertThat(analysisJob.getStatus())
                .isEqualTo(AnalysisJobStatus.COMPLETED);
        assertThat(analysisJob.getResult())
                .isEqualTo(result);
        assertThat(analysisJob.getErrorMessage()).isNull();
        assertThat(analysisJob.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("RUNNING 상태의 작업을 실패 처리하면 FAILED 상태와 오류 메시지를 저장한다")
    void failJob() {
        AnalysisJob analysisJob = createAnalysisJob();
        analysisJob.start();

        analysisJob.fail("GitHub API 호출에 실패했습니다.");

        assertThat(analysisJob.getStatus())
                .isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(analysisJob.getResult()).isNull();
        assertThat(analysisJob.getErrorMessage())
                .isEqualTo("GitHub API 호출에 실패했습니다.");
        assertThat(analysisJob.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 작업은 다시 시작할 수 없다")
    void cannotStartNonPendingJob() {
        AnalysisJob analysisJob = createAnalysisJob();
        analysisJob.start();

        assertThatThrownBy(analysisJob::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PENDING 상태의 분석 작업만 시작할 수 있습니다.");
    }

    @Test
    @DisplayName("RUNNING 상태가 아닌 작업은 완료할 수 없다")
    void cannotCompleteNonRunningJob() {
        AnalysisJob analysisJob = createAnalysisJob();

        assertThatThrownBy(() ->
                analysisJob.complete("""
                        {
                          "commitCount": 0,
                          "commits": []
                        }
                        """)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RUNNING 상태의 분석 작업만 완료할 수 있습니다.");
    }

    @Test
    @DisplayName("RUNNING 상태가 아닌 작업은 실패 처리할 수 없다")
    void cannotFailNonRunningJob() {
        AnalysisJob analysisJob = createAnalysisJob();

        assertThatThrownBy(() ->
                analysisJob.fail("오류")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RUNNING 상태의 분석 작업만 실패 처리할 수 있습니다.");
    }

    @Test
    @DisplayName("빈 분석 결과로 작업을 완료할 수 없다")
    void cannotCompleteWithBlankResult() {
        AnalysisJob analysisJob = createAnalysisJob();
        analysisJob.start();

        assertThatThrownBy(() ->
                analysisJob.complete("   ")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분석 결과는 필수입니다.");
    }

    @Test
    @DisplayName("빈 오류 메시지는 기본 오류 메시지로 변경한다")
    void normalizeBlankErrorMessage() {
        AnalysisJob analysisJob = createAnalysisJob();
        analysisJob.start();

        analysisJob.fail("   ");

        assertThat(analysisJob.getErrorMessage())
                .isEqualTo("분석 중 알 수 없는 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("1000자를 초과하는 오류 메시지는 1000자로 잘라 저장한다")
    void truncateLongErrorMessage() {
        AnalysisJob analysisJob = createAnalysisJob();
        analysisJob.start();

        String longMessage = "a".repeat(1200);

        analysisJob.fail(longMessage);

        assertThat(analysisJob.getErrorMessage())
                .hasSize(1000);
    }

    private AnalysisJob createAnalysisJob() {
        return new AnalysisJob(
                connectedRepository,
                LocalDate.of(2026, 7, 29)
        );
    }
}