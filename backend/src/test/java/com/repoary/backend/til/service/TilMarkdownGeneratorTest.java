package com.repoary.backend.til.service;

import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.repository.domain.ConnectedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TilMarkdownGeneratorTest {

    private TilMarkdownGenerator tilMarkdownGenerator;
    private ConnectedRepository connectedRepository;

    @BeforeEach
    void setUp() {
        tilMarkdownGenerator = new TilMarkdownGenerator();

        connectedRepository = mock(ConnectedRepository.class);

        when(connectedRepository.getFullName())
                .thenReturn("dPdms21/programmers-devcourse-be11");

        when(connectedRepository.getDefaultBranch())
                .thenReturn("main");
    }

    @Test
    @DisplayName("커밋 메시지의 날짜를 제거해 TIL 초안을 생성한다")
    void removeDateFromCommitMessage() {
        StoredAnalysisResult analysisResult =
                new StoredAnalysisResult(
                        LocalDate.of(2026, 8, 28),
                        1,
                        List.of(
                                createCommit(
                                        "study(java): 2026-08-28 MSA 서비스 분리 실습",
                                        List.of("practice"),
                                        List.of(
                                                createFile(
                                                        "practice/msa/board-service/src/main/java/BoardApplication.java",
                                                        "practice"
                                                )
                                        )
                                )
                        )
                );

        String markdown =
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                );

        assertThat(markdown)
                .contains("MSA 서비스 분리 실습")
                .doesNotContain("2026-08-28 MSA 서비스 분리 실습");
    }

    @Test
    @DisplayName("practice 파일은 학습 주제 폴더 단위로 링크한다")
    void createPracticeDirectoryLink() {
        StoredAnalysisResult analysisResult =
                new StoredAnalysisResult(
                        LocalDate.of(2026, 8, 28),
                        1,
                        List.of(
                                createCommit(
                                        "study(java): 2026-08-28 MSA 서비스 분리 실습",
                                        List.of("practice"),
                                        List.of(
                                                createFile(
                                                        "practice/msa/board-service/src/main/java/BoardApplication.java",
                                                        "practice"
                                                )
                                        )
                                )
                        )
                );

        String markdown =
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                );

        assertThat(markdown)
                .contains(
                        "[🔗 msa](https://github.com/dPdms21/programmers-devcourse-be11/tree/main/practice/msa)"
                );
    }

    @Test
    @DisplayName("codingtest의 단일 문제 파일은 해당 파일로 직접 링크한다")
    void createCodingTestFileLink() {
        StoredAnalysisResult analysisResult =
                new StoredAnalysisResult(
                        LocalDate.of(2026, 8, 28),
                        1,
                        List.of(
                                createCommit(
                                        "solve(java): 2026-08-28 프로세스 풀이",
                                        List.of("codingtest"),
                                        List.of(
                                                createFile(
                                                        "codingtest/programmers/queue/프로세스.java",
                                                        "codingtest"
                                                )
                                        )
                                )
                        )
                );

        String markdown =
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                );

        assertThat(markdown)
                .contains("[🔗 프로세스]")
                .contains(
                        "/blob/main/codingtest/programmers/queue/%ED%94%84%EB%A1%9C%EC%84%B8%EC%8A%A4.java"
                );
    }

    @Test
    @DisplayName("오늘 배운 내용은 불렛 없이 줄글로 생성한다")
    void createLearnedContentWithoutBullet() {
        StoredAnalysisResult analysisResult =
                new StoredAnalysisResult(
                        LocalDate.of(2026, 8, 28),
                        1,
                        List.of(
                                createCommit(
                                        "study(java): 2026-08-28 MSA 서비스 분리 실습",
                                        List.of("practice"),
                                        List.of(
                                                createFile(
                                                        "practice/msa/board-service/src/main/java/BoardApplication.java",
                                                        "practice"
                                                )
                                        )
                                )
                        )
                );

        String markdown =
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                );

        assertThat(markdown)
                .contains("""
                        ## 오늘 배운 내용

                        MSA 서비스 분리 실습 관련 내용을 학습했다.
                        """)
                .doesNotContain("""
                        ## 오늘 배운 내용

                        - MSA 서비스 분리 실습
                        """);
    }

    @Test
    @DisplayName("근거가 없는 TIL 항목은 내용을 임의로 생성하지 않는다")
    void keepUnsupportedSectionsEmpty() {
        StoredAnalysisResult analysisResult =
                new StoredAnalysisResult(
                        LocalDate.of(2026, 8, 28),
                        0,
                        List.of()
                );

        String markdown =
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                );

        assertThat(markdown)
                .contains("""
                        ## 헷갈렸던 점

                        ## 오늘 느낀 점

                        ## 추가 학습 예정
                        """);
    }

    private StoredAnalysisResult.StoredCommitAnalysis createCommit(
            String message,
            List<String> categories,
            List<StoredAnalysisResult.StoredFileAnalysis> files
    ) {
        return new StoredAnalysisResult.StoredCommitAnalysis(
                "abc123",
                message,
                Instant.parse("2026-08-28T03:00:00Z"),
                "study",
                "java",
                categories,
                files
        );
    }

    private StoredAnalysisResult.StoredFileAnalysis createFile(
            String filename,
            String category
    ) {
        return new StoredAnalysisResult.StoredFileAnalysis(
                filename,
                "modified",
                null,
                category,
                null
        );
    }
}