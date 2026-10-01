package com.repoary.backend.rule.preset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRulePresetTest {

    @Test
    @DisplayName("docs(practice) 기본 규칙은 practice를 scope와 category로 사용한다")
    void docsPracticeUsesPracticeAsScopeAndCategory() {
        DefaultRulePreset.ConventionRulePreset preset =
                DefaultRulePreset.getConventionRules().stream()
                        .filter(rule -> rule.messagePattern()
                                .equals("docs(practice):"))
                        .findFirst()
                        .orElseThrow();

        assertThat(preset.commitType()).isEqualTo("docs");
        assertThat(preset.scope()).isEqualTo("practice");
        assertThat(preset.category()).isEqualTo("practice");
    }

    @Test
    @DisplayName("docs 기본 규칙은 기존 TIL category를 유지한다")
    void docsRulesPreserveTilCategories() {
        assertRule("docs(lectures):", null, "lectures");
        assertRule("docs(assignments):", null, "assignments");
        assertRule("docs(til):", null, "til");
        assertRule("docs(practice):", "practice", "practice");
    }

    private void assertRule(
            String messagePattern,
            String scope,
            String category
    ) {
        DefaultRulePreset.ConventionRulePreset preset =
                DefaultRulePreset.getConventionRules().stream()
                        .filter(rule -> rule.messagePattern()
                                .equals(messagePattern))
                        .findFirst()
                        .orElseThrow();

        assertThat(preset.commitType()).isEqualTo("docs");
        assertThat(preset.scope()).isEqualTo(scope);
        assertThat(preset.category()).isEqualTo(category);
    }
}
