package com.repoary.backend.rule.dto;

import com.repoary.backend.rule.domain.ConventionRule;

import java.time.LocalDateTime;

public record ConventionRuleResponse(
        Long id,
        String messagePattern,
        String commitType,
        String scope,
        String category,
        Integer priority,
        boolean enabled,
        boolean defaultRule,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConventionRuleResponse from(
            ConventionRule rule
    ) {
        return new ConventionRuleResponse(
                rule.getId(),
                rule.getMessagePattern(),
                rule.getCommitType(),
                rule.getScope(),
                rule.getCategory(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.isDefaultRule(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}