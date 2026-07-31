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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommitConsistencyService {

    private static final Pattern CONVENTIONAL_COMMIT_PATTERN =
            Pattern.compile(
                    "^([A-Za-z]+)(?:\\(([^)]+)\\))?:\\s+(.+)$"
            );

    private final GitHubCommitService gitHubCommitService;
    private final RepositoryRuleMatcher repositoryRuleMatcher;
    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ConventionRuleRepository conventionRuleRepository;

    public CommitConsistencyService(
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
        this.connectedRepositoryRepository =
                connectedRepositoryRepository;
        this.classificationRuleRepository =
                classificationRuleRepository;
        this.conventionRuleRepository =
                conventionRuleRepository;
    }

    @Transactional(readOnly = true)
    public CommitConsistencyResponse analyze(
            Long userId,
            Long connectedRepositoryId,
            LocalDate from,
            LocalDate to
    ) {
        validatePeriod(from, to);

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
                                from,
                                to
                        )
                        .stream()
                        .filter(commit ->
                                !isMergeCommit(commit.message())
                        )
                        .toList();

        Map<GroupKey, List<AnalyzedCommit>> groupedCommits =
                new LinkedHashMap<>();

        for (GitHubCommitResponse commit : commits) {
            AnalyzedCommit analyzedCommit = analyzeCommit(
                    userId,
                    connectedRepositoryId,
                    commit,
                    classificationRules,
                    conventionRules
            );

            groupedCommits
                    .computeIfAbsent(
                            analyzedCommit.groupKey(),
                            ignored -> new ArrayList<>()
                    )
                    .add(analyzedCommit);
        }

        List<ConsistencyGroupResponse> groups =
                groupedCommits.entrySet()
                        .stream()
                        .map(entry ->
                                createGroupResponse(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                ConsistencyGroupResponse::pathPattern,
                                                Comparator.nullsLast(
                                                        String::compareTo
                                                )
                                        )
                                        .thenComparing(
                                                ConsistencyGroupResponse::expectedPattern,
                                                Comparator.nullsLast(
                                                        String::compareTo
                                                )
                                        )
                        )
                        .toList();

        int consistentCount = groups.stream()
                .mapToInt(
                        ConsistencyGroupResponse::consistentCount
                )
                .sum();

        int inconsistentCount = groups.stream()
                .mapToInt(
                        ConsistencyGroupResponse::inconsistentCount
                )
                .sum();

        return new CommitConsistencyResponse(
                from,
                to,
                commits.size(),
                consistentCount,
                inconsistentCount,
                groups
        );
    }

    private AnalyzedCommit analyzeCommit(
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

        ClassificationMatchResult representativeClassification =
                findRepresentativeClassification(
                        detail.files(),
                        classificationRules
                ).orElse(null);

        ConventionMatchResult matchedConvention =
                repositoryRuleMatcher
                        .matchConventionRule(
                                commit.message(),
                                conventionRules
                        )
                        .orElse(null);

        GroupKey groupKey = createGroupKey(
                representativeClassification,
                matchedConvention
        );

        return new AnalyzedCommit(
                commit,
                groupKey,
                matchedConvention
        );
    }

    private Optional<ClassificationMatchResult>
    findRepresentativeClassification(
            List<GitHubCommitDetailResponse.ChangedFile> files,
            List<ClassificationRule> classificationRules
    ) {
        Map<ClassificationMatchResult, Long> matchCounts =
                files.stream()
                        .map(file ->
                                repositoryRuleMatcher
                                        .matchClassificationRule(
                                                file.filename(),
                                                classificationRules
                                        )
                                        .orElse(null)
                        )
                        .filter(Objects::nonNull)
                        .collect(
                                Collectors.groupingBy(
                                        match -> match,
                                        LinkedHashMap::new,
                                        Collectors.counting()
                                )
                        );

        return matchCounts.entrySet()
                .stream()
                .sorted(
                        Comparator
                                .<Map.Entry<
                                        ClassificationMatchResult,
                                        Long
                                        >>comparingLong(
                                        Map.Entry::getValue
                                )
                                .reversed()
                                .thenComparing(
                                        entry ->
                                                calculatePathSpecificity(
                                                        entry.getKey()
                                                                .pathPattern()
                                                ),
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        entry ->
                                                entry.getKey()
                                                        .priority()
                                )
                )
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private GroupKey createGroupKey(
            ClassificationMatchResult classification,
            ConventionMatchResult convention
    ) {
        String pathPattern = classification == null
                ? null
                : classification.pathPattern();

        String category = classification == null
                ? "unclassified"
                : classification.category();

        String classificationScope = classification == null
                ? null
                : classification.scope();

        Long conventionRuleId = convention == null
                ? null
                : convention.ruleId();

        String conventionPattern = convention == null
                ? null
                : convention.messagePattern();

        String conventionScope = convention == null
                ? null
                : convention.scope();

        return new GroupKey(
                pathPattern,
                category,
                classificationScope,
                conventionRuleId,
                conventionPattern,
                conventionScope
        );
    }

    private ConsistencyGroupResponse createGroupResponse(
            GroupKey groupKey,
            List<AnalyzedCommit> commits
    ) {
        ConventionMatchResult expectedConvention =
                commits.stream()
                        .map(AnalyzedCommit::matchedConvention)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

        List<ConsistencyCommitResponse> commitResponses =
                commits.stream()
                        .map(commit ->
                                createCommitResponse(
                                        commit,
                                        expectedConvention
                                )
                        )
                        .toList();

        int consistentCount = (int) commitResponses.stream()
                .filter(
                        ConsistencyCommitResponse::consistent
                )
                .count();

        int inconsistentCount =
                commitResponses.size() - consistentCount;

        String responseScope =
                groupKey.conventionScope() != null
                        ? groupKey.conventionScope()
                        : groupKey.classificationScope();

        return new ConsistencyGroupResponse(
                groupKey.pathPattern(),
                groupKey.category(),
                responseScope,
                groupKey.conventionPattern(),
                commitResponses.size(),
                consistentCount,
                inconsistentCount,
                commitResponses
        );
    }

    private ConsistencyCommitResponse createCommitResponse(
            AnalyzedCommit analyzedCommit,
            ConventionMatchResult expectedConvention
    ) {
        GitHubCommitResponse commit =
                analyzedCommit.commit();

        String firstLine =
                getFirstLine(commit.message());

        ParsedCommitMessage parsedMessage =
                parseCommitMessage(firstLine);

        List<String> issues = new ArrayList<>();

        if (expectedConvention == null) {
            issues.add(
                    "적용 가능한 커밋 규칙이 없습니다."
            );
        } else {
            validateExpectedConvention(
                    firstLine,
                    parsedMessage,
                    expectedConvention,
                    issues
            );
        }

        String responseScope =
                parsedMessage.scope() != null
                        ? parsedMessage.scope()
                        : analyzedCommit
                        .groupKey()
                        .classificationScope();

        return new ConsistencyCommitResponse(
                commit.sha(),
                commit.message(),
                commit.htmlUrl(),
                commit.committedAt(),
                parsedMessage.commitType(),
                responseScope,
                analyzedCommit.groupKey().category(),
                issues.isEmpty(),
                List.copyOf(issues)
        );
    }

    private void validateExpectedConvention(
            String firstLine,
            ParsedCommitMessage parsedMessage,
            ConventionMatchResult expectedConvention,
            List<String> issues
    ) {
        if (parsedMessage.commitType() == null) {
            issues.add(
                    "커밋 메시지가 type(scope): 제목 형식과 일치하지 않습니다."
            );
            return;
        }

        if (
                expectedConvention.commitType() != null
                        && !expectedConvention
                        .commitType()
                        .equalsIgnoreCase(
                                parsedMessage.commitType()
                        )
        ) {
            issues.add(
                    "type이 권장 값인 "
                            + expectedConvention.commitType()
                            + "와 일치하지 않습니다."
            );
        }

        if (
                expectedConvention.scope() != null
                        && !Objects.equals(
                        expectedConvention.scope(),
                        parsedMessage.scope()
                )
        ) {
            issues.add(
                    "scope가 권장 값인 "
                            + expectedConvention.scope()
                            + "와 일치하지 않습니다."
            );
        }

        if (
                expectedConvention.messagePattern() != null
                        && !firstLine.startsWith(
                        expectedConvention
                                .messagePattern()
                                .trim()
                )
        ) {
            issues.add(
                    "커밋 메시지가 권장 형식인 "
                            + expectedConvention.messagePattern()
                            + "로 시작하지 않습니다."
            );
        }
    }

    private ParsedCommitMessage parseCommitMessage(
            String firstLine
    ) {
        Matcher matcher =
                CONVENTIONAL_COMMIT_PATTERN.matcher(firstLine);

        if (!matcher.matches()) {
            return new ParsedCommitMessage(
                    null,
                    null
            );
        }

        return new ParsedCommitMessage(
                matcher.group(1),
                matcher.group(2)
        );
    }

    private String getFirstLine(String message) {
        if (message == null) {
            return "";
        }

        return message.lines()
                .findFirst()
                .orElse("")
                .trim();
    }

    private boolean isMergeCommit(String message) {
        return getFirstLine(message)
                .startsWith("Merge ");
    }

    private int calculatePathSpecificity(
            String pathPattern
    ) {
        if (pathPattern == null || pathPattern.isBlank()) {
            return 0;
        }

        return (int) List.of(
                        pathPattern
                                .replace('\\', '/')
                                .replace("/**", "")
                                .split("/")
                )
                .stream()
                .filter(segment ->
                        !segment.isBlank()
                )
                .count();
    }

    private void validatePeriod(
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "분석 시작일과 종료일은 필수입니다."
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "분석 시작일은 종료일보다 늦을 수 없습니다."
            );
        }
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

    private record GroupKey(
            String pathPattern,
            String category,
            String classificationScope,
            Long conventionRuleId,
            String conventionPattern,
            String conventionScope
    ) {
    }

    private record AnalyzedCommit(
            GitHubCommitResponse commit,
            GroupKey groupKey,
            ConventionMatchResult matchedConvention
    ) {
    }

    private record ParsedCommitMessage(
            String commitType,
            String scope
    ) {
    }
}