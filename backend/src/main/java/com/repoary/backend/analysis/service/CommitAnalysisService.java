package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.ClassificationMatchResult;
import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CommitAnalysisService {

    private final GitHubCommitService gitHubCommitService;
    private final RepositoryRuleMatcher repositoryRuleMatcher;
    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ConventionRuleRepository conventionRuleRepository;

    public CommitAnalysisService(
            GitHubCommitService gitHubCommitService,
            RepositoryRuleMatcher repositoryRuleMatcher,
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            ClassificationRuleRepository classificationRuleRepository,
            ConventionRuleRepository conventionRuleRepository
    ) {
        this.gitHubCommitService = gitHubCommitService;
        this.repositoryRuleMatcher = repositoryRuleMatcher;
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.classificationRuleRepository = classificationRuleRepository;
        this.conventionRuleRepository = conventionRuleRepository;
    }

    public List<CommitAnalysisResponse> analyzeCommits(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        List<ClassificationRule> classificationRules =
                classificationRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        );

        List<ConventionRule> conventionRules =
                conventionRuleRepository
                        .findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
                                connectedRepository
                        );

        List<GitHubCommitResponse> commits =
                gitHubCommitService.getCommits(
                        userId,
                        connectedRepositoryId,
                        targetDate
                );

        return commits.stream()
                .map(commit -> analyzeCommit(
                        userId,
                        connectedRepositoryId,
                        commit,
                        classificationRules,
                        conventionRules
                ))
                .toList();
    }

    private CommitAnalysisResponse analyzeCommit(
            Long userId,
            Long connectedRepositoryId,
            GitHubCommitResponse commit,
            List<ClassificationRule> classificationRules,
            List<ConventionRule> conventionRules
    ) {
        GitHubCommitDetailResponse detail =
                gitHubCommitService.getCommitDetail(
                        userId,
                        connectedRepositoryId,
                        commit.sha()
                );

        ConventionMatchResult convention =
                repositoryRuleMatcher
                        .matchConventionRule(
                                commit.message(),
                                conventionRules
                        )
                        .orElse(null);

        List<CommitAnalysisResponse.FileAnalysis> files =
                detail.files()
                        .stream()
                        .map(file -> analyzeFile(
                                file,
                                classificationRules
                        ))
                        .toList();

        return new CommitAnalysisResponse(
                commit.sha(),
                commit.message(),
                commit.htmlUrl(),
                commit.committedAt(),
                convention,
                files
        );
    }

    private CommitAnalysisResponse.FileAnalysis analyzeFile(
            GitHubCommitDetailResponse.ChangedFile file,
            List<ClassificationRule> classificationRules
    ) {
        ClassificationMatchResult classification =
                repositoryRuleMatcher
                        .matchClassificationRule(
                                file.filename(),
                                classificationRules
                        )
                        .orElse(null);

        return new CommitAnalysisResponse.FileAnalysis(
                file.filename(),
                file.status(),
                file.additions(),
                file.deletions(),
                file.changes(),
                file.previousFilename(),
                classification
        );
    }

    private ConnectedRepository getOwnedConnectedRepository(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return connectedRepositoryRepository
                .findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "연결된 저장소를 찾을 수 없습니다."
                        )
                );
    }
}