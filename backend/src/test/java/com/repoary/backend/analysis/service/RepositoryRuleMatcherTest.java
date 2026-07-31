package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.dto.ClassificationMatchResult;
import com.repoary.backend.analysis.dto.ConventionMatchResult;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepositoryRuleMatcherTest {

    private final RepositoryRuleMatcher repositoryRuleMatcher =
            new RepositoryRuleMatcher();

    @Test
    @DisplayName("파일 경로와 일치하는 분류 규칙을 반환한다")
    void matchClassificationRule() {
        ClassificationRule rule = classificationRule(
                1L,
                "practice/**",
                "practice",
                "springboot",
                100,
                true
        );

        Optional<ClassificationMatchResult> result =
                repositoryRuleMatcher.matchClassificationRule(
                        "practice/springboot/security/SecurityConfig.java",
                        List.of(rule)
                );

        assertThat(result).isPresent();
        assertThat(result.get().ruleId()).isEqualTo(1L);
        assertThat(result.get().pathPattern()).isEqualTo("practice/**");
        assertThat(result.get().category()).isEqualTo("practice");
        assertThat(result.get().scope()).isEqualTo("springboot");
    }

    @Test
    @DisplayName("여러 분류 규칙이 일치하면 더 구체적인 경로를 우선한다")
    void preferMoreSpecificClassificationRule() {
        ClassificationRule broadRule = classificationRule(
                1L,
                "practice/**",
                "practice",
                null,
                10,
                true
        );

        ClassificationRule specificRule = classificationRule(
                2L,
                "practice/springboot/security/**",
                "security",
                "springboot",
                100,
                true
        );

        Optional<ClassificationMatchResult> result =
                repositoryRuleMatcher.matchClassificationRule(
                        "practice/springboot/security/token/TokenProvider.java",
                        List.of(broadRule, specificRule)
                );

        assertThat(result).isPresent();
        assertThat(result.get().ruleId()).isEqualTo(2L);
        assertThat(result.get().pathPattern())
                .isEqualTo("practice/springboot/security/**");
    }

    @Test
    @DisplayName("경로 구체성이 같으면 priority가 낮은 분류 규칙을 우선한다")
    void preferLowerPriorityClassificationRule() {
        ClassificationRule lowerPriorityRule = classificationRule(
                1L,
                "practice/**",
                "first-category",
                null,
                10,
                true
        );

        ClassificationRule higherPriorityRule = classificationRule(
                2L,
                "practice/**",
                "second-category",
                null,
                100,
                true
        );

        Optional<ClassificationMatchResult> result =
                repositoryRuleMatcher.matchClassificationRule(
                        "practice/java/CollectionTest.java",
                        List.of(higherPriorityRule, lowerPriorityRule)
                );

        assertThat(result).isPresent();
        assertThat(result.get().ruleId()).isEqualTo(1L);
        assertThat(result.get().priority()).isEqualTo(10);
    }

    @Test
    @DisplayName("비활성화된 분류 규칙은 적용하지 않는다")
    void ignoreDisabledClassificationRule() {
        ClassificationRule disabledRule = classificationRule(
                1L,
                "practice/**",
                "practice",
                null,
                1,
                false
        );

        Optional<ClassificationMatchResult> result =
                repositoryRuleMatcher.matchClassificationRule(
                        "practice/java/Main.java",
                        List.of(disabledRule)
                );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("커밋 메시지 접두어와 일치하는 컨벤션 규칙을 반환한다")
    void matchConventionRule() {
        ConventionRule rule = conventionRule(
                1L,
                "study(springboot):",
                "study",
                "springboot",
                null,
                100,
                true
        );

        Optional<ConventionMatchResult> result =
                repositoryRuleMatcher.matchConventionRule(
                        "study(springboot): 2026-07-29 JWT 토큰 인증 실습",
                        List.of(rule)
                );

        assertThat(result).isPresent();
        assertThat(result.get().ruleId()).isEqualTo(1L);
        assertThat(result.get().commitType()).isEqualTo("study");
        assertThat(result.get().scope()).isEqualTo("springboot");
    }

    @Test
    @DisplayName("여러 컨벤션 규칙이 일치하면 더 긴 메시지 패턴을 우선한다")
    void preferMoreSpecificConventionRule() {
        ConventionRule broadRule = conventionRule(
                1L,
                "docs(",
                "docs",
                null,
                null,
                10,
                true
        );

        ConventionRule specificRule = conventionRule(
                2L,
                "docs(til):",
                "docs",
                null,
                "til",
                100,
                true
        );

        Optional<ConventionMatchResult> result =
                repositoryRuleMatcher.matchConventionRule(
                        "docs(til): 2026-07-29 학습 회고 정리",
                        List.of(broadRule, specificRule)
                );

        assertThat(result).isPresent();
        assertThat(result.get().ruleId()).isEqualTo(2L);
        assertThat(result.get().category()).isEqualTo("til");
    }

    @Test
    @DisplayName("여러 줄 커밋 메시지는 첫 번째 줄을 기준으로 분석한다")
    void matchFirstLineOfCommitMessage() {
        ConventionRule rule = conventionRule(
                1L,
                "study(java):",
                "study",
                "java",
                null,
                100,
                true
        );

        Optional<ConventionMatchResult> result =
                repositoryRuleMatcher.matchConventionRule(
                        """
                        study(java): 컬렉션 실습

                        List와 Map 사용 방법을 정리한다.
                        """,
                        List.of(rule)
                );

        assertThat(result).isPresent();
        assertThat(result.get().commitType()).isEqualTo("study");
        assertThat(result.get().scope()).isEqualTo("java");
    }

    @Test
    @DisplayName("비활성화된 컨벤션 규칙은 적용하지 않는다")
    void ignoreDisabledConventionRule() {
        ConventionRule disabledRule = conventionRule(
                1L,
                "study(java):",
                "study",
                "java",
                null,
                1,
                false
        );

        Optional<ConventionMatchResult> result =
                repositoryRuleMatcher.matchConventionRule(
                        "study(java): 컬렉션 실습",
                        List.of(disabledRule)
                );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일치하는 규칙이 없으면 빈 결과를 반환한다")
    void returnEmptyWhenNoRuleMatches() {
        ClassificationRule classificationRule = classificationRule(
                1L,
                "assignments/**",
                "assignments",
                null,
                100,
                true
        );

        ConventionRule conventionRule = conventionRule(
                2L,
                "solve(java):",
                "solve",
                "java",
                "codingtest",
                100,
                true
        );

        assertThat(
                repositoryRuleMatcher.matchClassificationRule(
                        "practice/java/Main.java",
                        List.of(classificationRule)
                )
        ).isEmpty();

        assertThat(
                repositoryRuleMatcher.matchConventionRule(
                        "study(java): 컬렉션 실습",
                        List.of(conventionRule)
                )
        ).isEmpty();
    }

    private ClassificationRule classificationRule(
            Long id,
            String pathPattern,
            String category,
            String scope,
            int priority,
            boolean enabled
    ) {
        ClassificationRule rule = mock(ClassificationRule.class);

        when(rule.getId()).thenReturn(id);
        when(rule.getPathPattern()).thenReturn(pathPattern);
        when(rule.getCategory()).thenReturn(category);
        when(rule.getScope()).thenReturn(scope);
        when(rule.getPriority()).thenReturn(priority);
        when(rule.isEnabled()).thenReturn(enabled);

        return rule;
    }

    private ConventionRule conventionRule(
            Long id,
            String messagePattern,
            String commitType,
            String scope,
            String category,
            int priority,
            boolean enabled
    ) {
        ConventionRule rule = mock(ConventionRule.class);

        when(rule.getId()).thenReturn(id);
        when(rule.getMessagePattern()).thenReturn(messagePattern);
        when(rule.getCommitType()).thenReturn(commitType);
        when(rule.getScope()).thenReturn(scope);
        when(rule.getCategory()).thenReturn(category);
        when(rule.getPriority()).thenReturn(priority);
        when(rule.isEnabled()).thenReturn(enabled);

        return rule;
    }
}