package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.ClassificationMatchResult;
import com.repoary.backend.analysis.dto.ConventionMatchResult;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class RepositoryRuleMatcher {

    public Optional<ClassificationMatchResult> matchClassificationRule(
            String filePath,
            List<ClassificationRule> rules
    ) {
        if (filePath == null || filePath.isBlank()) {
            return Optional.empty();
        }

        String normalizedFilePath = normalizePath(filePath);

        return rules.stream()
                .filter(ClassificationRule::isEnabled)
                .filter(rule -> matchesPath(
                        normalizedFilePath,
                        rule.getPathPattern()
                ))
                .sorted(
                        Comparator
                                .comparingInt(
                                        (ClassificationRule rule) ->
                                                calculateSpecificity(
                                                        rule.getPathPattern()
                                                )
                                )
                                .reversed()
                                .thenComparingInt(
                                        ClassificationRule::getPriority
                                )
                )
                .findFirst()
                .map(rule -> new ClassificationMatchResult(
                        rule.getId(),
                        rule.getPathPattern(),
                        rule.getCategory(),
                        rule.getScope(),
                        rule.getPriority()
                ));
    }

    public Optional<ConventionMatchResult> matchConventionRule(
            String commitMessage,
            List<ConventionRule> rules
    ) {
        if (commitMessage == null || commitMessage.isBlank()) {
            return Optional.empty();
        }

        String firstLine = commitMessage
                .lines()
                .findFirst()
                .orElse("")
                .trim();

        return rules.stream()
                .filter(ConventionRule::isEnabled)
                .filter(rule -> matchesMessage(
                        firstLine,
                        rule.getMessagePattern()
                ))
                .sorted(
                        Comparator
                                .comparingInt(
                                        (ConventionRule rule) ->
                                                rule.getMessagePattern().length()
                                )
                                .reversed()
                                .thenComparingInt(
                                        ConventionRule::getPriority
                                )
                )
                .findFirst()
                .map(rule -> new ConventionMatchResult(
                        rule.getId(),
                        rule.getMessagePattern(),
                        rule.getCommitType(),
                        rule.getScope(),
                        rule.getCategory(),
                        rule.getPriority()
                ));
    }

    private boolean matchesPath(
            String normalizedFilePath,
            String pathPattern
    ) {
        if (pathPattern == null || pathPattern.isBlank()) {
            return false;
        }

        String normalizedPattern = normalizePath(pathPattern);

        if (normalizedPattern.endsWith("/**")) {
            normalizedPattern = normalizedPattern.substring(
                    0,
                    normalizedPattern.length() - 3
            );
        }

        if (normalizedPattern.endsWith("/")) {
            normalizedPattern = normalizedPattern.substring(
                    0,
                    normalizedPattern.length() - 1
            );
        }

        return normalizedFilePath.equals(normalizedPattern)
                || normalizedFilePath.startsWith(normalizedPattern + "/");
    }

    private boolean matchesMessage(
            String commitMessage,
            String messagePattern
    ) {
        if (messagePattern == null || messagePattern.isBlank()) {
            return false;
        }

        return commitMessage.startsWith(messagePattern.trim());
    }

    private int calculateSpecificity(String pathPattern) {
        if (pathPattern == null || pathPattern.isBlank()) {
            return 0;
        }

        String normalizedPattern = normalizePath(pathPattern)
                .replace("/**", "");

        return (int) List.of(normalizedPattern.split("/"))
                .stream()
                .filter(segment -> !segment.isBlank())
                .count();
    }

    private String normalizePath(String path) {
        String normalized = path
                .trim()
                .replace('\\', '/');

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }

        return normalized;
    }
}