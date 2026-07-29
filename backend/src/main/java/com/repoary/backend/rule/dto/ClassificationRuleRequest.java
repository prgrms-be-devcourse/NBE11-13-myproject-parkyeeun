package com.repoary.backend.rule.dto;

public record ClassificationRuleRequest(
        String pathPattern,
        String category,
        String scope,
        Integer priority
) {
}