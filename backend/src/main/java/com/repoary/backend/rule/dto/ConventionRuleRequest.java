package com.repoary.backend.rule.dto;

public record ConventionRuleRequest(
        String messagePattern,
        String commitType,
        String scope,
        String category,
        Integer priority
) {
}