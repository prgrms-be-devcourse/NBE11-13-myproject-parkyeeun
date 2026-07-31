package com.repoary.backend.analysis.dto;

public record ClassificationMatchResult(
        Long ruleId,
        String pathPattern,
        String category,
        String scope,
        int priority
) {
}