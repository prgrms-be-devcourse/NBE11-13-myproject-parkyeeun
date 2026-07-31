package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.ClassificationMatchResult;
import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
import com.repoary.backend.analysis.dto.ConventionMatchResult;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class StoredAnalysisResultMapper {

    private final AnalysisFileFilter analysisFileFilter;

    public StoredAnalysisResultMapper(
            AnalysisFileFilter analysisFileFilter
    ) {
        this.analysisFileFilter = analysisFileFilter;
    }

    public StoredAnalysisResult map(
            LocalDate targetDate,
            List<CommitAnalysisResponse> analysisResponses
    ) {
        List<StoredAnalysisResult.StoredCommitAnalysis> commits =
                analysisResponses.stream()
                        .filter(this::isTilSourceCommit)
                        .map(this::mapCommit)
                        .filter(this::hasMeaningfulAnalysisData)
                        .toList();

        return new StoredAnalysisResult(
                targetDate,
                commits.size(),
                commits
        );
    }

    private boolean isTilSourceCommit(
            CommitAnalysisResponse response
    ) {
        ConventionMatchResult convention = response.convention();

        if (convention == null) {
            return true;
        }

        /*
         * 이미 작성된 TIL 자체를 다시 TIL 생성 근거로 사용하지 않는다.
         */
        return !Objects.equals(
                convention.messagePattern(),
                "docs(til):"
        );
    }

    private StoredAnalysisResult.StoredCommitAnalysis mapCommit(
            CommitAnalysisResponse response
    ) {
        ConventionMatchResult convention = response.convention();

        List<StoredAnalysisResult.StoredFileAnalysis> files =
                response.files().stream()
                        .filter(file ->
                                analysisFileFilter.isRelevant(
                                        file.filename()
                                )
                        )
                        .map(this::mapFile)
                        .toList();

        List<String> categories =
                collectCategories(convention, files);

        return new StoredAnalysisResult.StoredCommitAnalysis(
                response.sha(),
                response.message(),
                response.committedAt(),
                convention == null
                        ? null
                        : convention.commitType(),
                convention == null
                        ? null
                        : convention.scope(),
                categories,
                files
        );
    }

    private StoredAnalysisResult.StoredFileAnalysis mapFile(
            CommitAnalysisResponse.FileAnalysis file
    ) {
        ClassificationMatchResult classification =
                file.classification();

        return new StoredAnalysisResult.StoredFileAnalysis(
                file.filename(),
                file.status(),
                file.previousFilename(),
                classification == null
                        ? null
                        : classification.category(),
                classification == null
                        ? null
                        : classification.scope()
        );
    }

    private List<String> collectCategories(
            ConventionMatchResult convention,
            List<StoredAnalysisResult.StoredFileAnalysis> files
    ) {
        Set<String> categories = new LinkedHashSet<>();

        if (convention != null
                && convention.category() != null
                && !convention.category().isBlank()) {
            categories.add(convention.category());
        }

        files.stream()
                .map(StoredAnalysisResult.StoredFileAnalysis::category)
                .filter(Objects::nonNull)
                .filter(category -> !category.isBlank())
                .forEach(categories::add);

        return List.copyOf(categories);
    }

    private boolean hasMeaningfulAnalysisData(
            StoredAnalysisResult.StoredCommitAnalysis commit
    ) {
        /*
         * 파일이 필터에서 모두 제외되더라도 분석된 커밋 메시지가 있으면
         * TIL 생성 근거로 유지한다.
         */
        return commit.commitType() != null
                || !commit.categories().isEmpty()
                || !commit.files().isEmpty();
    }
}