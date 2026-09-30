package com.repoary.backend.rule.preset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRulePresetTest {

    @Test
    @DisplayName("docs(practice) 기본 규칙은 practice를 scope로 저장한다")
    void docsPracticeUsesPracticeAsScope() {
        DefaultRulePreset.ConventionRulePreset preset =
                DefaultRulePreset.getConventionRules().stream()
                        .filter(rule -> rule.messagePattern()
                                .equals("docs(practice):"))
                        .findFirst()
                        .orElseThrow();

        assertThat(preset.commitType()).isEqualTo("docs");
        assertThat(preset.scope()).isEqualTo("practice");
        assertThat(preset.category()).isEqualTo("docs");
    }
}
