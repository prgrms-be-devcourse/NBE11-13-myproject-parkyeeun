package com.repoary.backend.rule.dto;

import com.repoary.backend.rule.domain.ClassificationRule;

import java.time.LocalDateTime;

public record ClassificationRuleResponse(
        Long id,
        String pathPattern,
        String category,
        String scope,
        Integer priority,
        boolean enabled,
        boolean defaultRule,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ClassificationRuleResponse from(
            ClassificationRule rule
    ) {
        return new ClassificationRuleResponse(
                rule.getId(),
                rule.getPathPattern(),
                rule.getCategory(),
                rule.getScope(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.isDefaultRule(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}