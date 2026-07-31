package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.ClassificationMatchResult;
import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
import com.repoary.backend.analysis.dto.ConventionMatchResult;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoredAnalysisResultMapperTest {

    private final StoredAnalysisResultMapper mapper =
            new StoredAnalysisResultMapper(
                    new AnalysisFileFilter()
            );

    @Test
    @DisplayName("상세 분석 결과를 TIL 생성용 축약 결과로 변환한다")
    void mapAnalysisResult() {
        CommitAnalysisResponse response = commit(
                "commit-sha",
                "study(springboot): JWT 인증 실습",
                convention(
                        "study(springboot):",
                        "study",
                        "springboot",
                        null
                ),
                List.of(
                        file(
                                "practice/springboot/security/TokenProvider.java",
                                "added",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        ),
                        file(
                                "practice/springboot/security/application.yml",
                                "modified",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(response)
        );

        assertThat(result.targetDate())
                .isEqualTo(LocalDate.of(2026, 7, 29));

        assertThat(result.commitCount()).isEqualTo(1);
        assertThat(result.commits()).hasSize(1);

        StoredAnalysisResult.StoredCommitAnalysis commit =
                result.commits().get(0);

        assertThat(commit.sha()).isEqualTo("commit-sha");
        assertThat(commit.commitType()).isEqualTo("study");
        assertThat(commit.scope()).isEqualTo("springboot");
        assertThat(commit.categories())
                .containsExactly("practice");
        assertThat(commit.files()).hasSize(2);
    }

    @Test
    @DisplayName("이미 작성된 docs til 커밋은 TIL 생성 근거에서 제외한다")
    void excludeDocsTilCommit() {
        CommitAnalysisResponse tilCommit = commit(
                "til-sha",
                "docs(til): 2026-07-29 학습 회고 정리",
                convention(
                        "docs(til):",
                        "docs",
                        null,
                        "til"
                ),
                List.of(
                        file(
                                "til/2026-07/2026-07-29.md",
                                "added",
                                null,
                                classification(
                                        "til/**",
                                        "til",
                                        null
                                )
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(tilCommit)
        );

        assertThat(result.commitCount()).isZero();
        assertThat(result.commits()).isEmpty();
    }

    @Test
    @DisplayName("TIL 작성에 불필요한 파일은 저장 결과에서 제외한다")
    void excludeIrrelevantFiles() {
        CommitAnalysisResponse response = commit(
                "commit-sha",
                "study(java): 컬렉션 실습",
                convention(
                        "study(java):",
                        "study",
                        "java",
                        null
                ),
                List.of(
                        file(
                                "practice/java/ListExample.java",
                                "added",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        ),
                        file(
                                "practice/java/gradlew",
                                "added",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        ),
                        file(
                                "practice/java/.gitignore",
                                "modified",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        ),
                        file(
                                "practice/java/gradle/wrapper/gradle-wrapper.jar",
                                "added",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(response)
        );

        assertThat(result.commits()).hasSize(1);
        assertThat(result.commits().get(0).files())
                .extracting(
                        StoredAnalysisResult.StoredFileAnalysis::filename
                )
                .containsExactly(
                        "practice/java/ListExample.java"
                );
    }

    @Test
    @DisplayName("컨벤션과 파일에서 수집한 카테고리는 중복 없이 유지한다")
    void collectDistinctCategories() {
        CommitAnalysisResponse response = commit(
                "commit-sha",
                "docs(practice): Security 정리",
                convention(
                        "docs(practice):",
                        "docs",
                        null,
                        "practice"
                ),
                List.of(
                        file(
                                "practice/security/SecurityConfig.java",
                                "added",
                                null,
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        ),
                        file(
                                "assignments/security/README.md",
                                "added",
                                null,
                                classification(
                                        "assignments/**",
                                        "assignments",
                                        null
                                )
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(response)
        );

        assertThat(result.commits().get(0).categories())
                .containsExactly(
                        "practice",
                        "assignments"
                );
    }

    @Test
    @DisplayName("핵심 파일이 없어도 컨벤션이 분석된 커밋은 유지한다")
    void keepCommitWhenConventionMatched() {
        CommitAnalysisResponse response = commit(
                "commit-sha",
                "chore(project): 프로젝트 초기 설정",
                convention(
                        "chore(project):",
                        "chore",
                        "project",
                        "project"
                ),
                List.of(
                        file(
                                "gradlew",
                                "added",
                                null,
                                null
                        ),
                        file(
                                ".gitignore",
                                "added",
                                null,
                                null
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(response)
        );

        assertThat(result.commitCount()).isEqualTo(1);
        assertThat(result.commits().get(0).files()).isEmpty();
        assertThat(result.commits().get(0).commitType())
                .isEqualTo("chore");
        assertThat(result.commits().get(0).categories())
                .containsExactly("project");
    }

    @Test
    @DisplayName("컨벤션도 없고 핵심 파일도 없는 커밋은 제외한다")
    void excludeCommitWithoutMeaningfulData() {
        CommitAnalysisResponse response = commit(
                "commit-sha",
                "임시 파일 정리",
                null,
                List.of(
                        file(
                                "gradlew",
                                "modified",
                                null,
                                null
                        ),
                        file(
                                ".gitattributes",
                                "modified",
                                null,
                                null
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(response)
        );

        assertThat(result.commitCount()).isZero();
        assertThat(result.commits()).isEmpty();
    }

    @Test
    @DisplayName("이름이 변경된 핵심 파일의 이전 경로를 저장한다")
    void preservePreviousFilename() {
        CommitAnalysisResponse response = commit(
                "commit-sha",
                "refactor(java): 패키지 구조 변경",
                convention(
                        "refactor(",
                        "refactor",
                        null,
                        null
                ),
                List.of(
                        file(
                                "practice/java/service/UserService.java",
                                "renamed",
                                "practice/java/UserService.java",
                                classification(
                                        "practice/**",
                                        "practice",
                                        null
                                )
                        )
                )
        );

        StoredAnalysisResult result = mapper.map(
                LocalDate.of(2026, 7, 29),
                List.of(response)
        );

        StoredAnalysisResult.StoredFileAnalysis file =
                result.commits().get(0).files().get(0);

        assertThat(file.status()).isEqualTo("renamed");
        assertThat(file.previousFilename())
                .isEqualTo("practice/java/UserService.java");
    }

    private CommitAnalysisResponse commit(
            String sha,
            String message,
            ConventionMatchResult convention,
            List<CommitAnalysisResponse.FileAnalysis> files
    ) {
        return new CommitAnalysisResponse(
                sha,
                message,
                "https://github.com/sample/repository/commit/" + sha,
                Instant.parse("2026-07-29T06:25:11Z"),
                convention,
                files
        );
    }

    private CommitAnalysisResponse.FileAnalysis file(
            String filename,
            String status,
            String previousFilename,
            ClassificationMatchResult classification
    ) {
        return new CommitAnalysisResponse.FileAnalysis(
                filename,
                status,
                10,
                2,
                12,
                previousFilename,
                classification
        );
    }

    private ConventionMatchResult convention(
            String messagePattern,
            String commitType,
            String scope,
            String category
    ) {
        return new ConventionMatchResult(
                1L,
                messagePattern,
                commitType,
                scope,
                category,
                100
        );
    }

    private ClassificationMatchResult classification(
            String pathPattern,
            String category,
            String scope
    ) {
        return new ClassificationMatchResult(
                1L,
                pathPattern,
                category,
                scope,
                100
        );
    }
}