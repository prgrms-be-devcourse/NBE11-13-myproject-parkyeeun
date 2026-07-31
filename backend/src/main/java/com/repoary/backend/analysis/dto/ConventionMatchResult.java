package com.repoary.backend.analysis.dto;

public record ConventionMatchResult(
        Long ruleId,
        String messagePattern,
        String commitType,
        String scope,
        String category,
        int priority
) {
}