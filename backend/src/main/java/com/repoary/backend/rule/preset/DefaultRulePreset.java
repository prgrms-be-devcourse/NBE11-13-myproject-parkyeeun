package com.repoary.backend.rule.preset;

import java.util.List;
import java.util.Map;

public final class DefaultRulePreset {

    public static final int DEFAULT_PRIORITY = 100;

    private static final Map<String, String> DIRECTORY_CATEGORIES = Map.of(
            "lectures", "lectures",
            "practice", "practice",
            "assignments", "assignments",
            "codingtest", "codingtest",
            "til", "til"
    );

    private static final List<ConventionRulePreset> CONVENTION_RULES = List.of(
            new ConventionRulePreset(
                    "docs(assignments):",
                    "docs",
                    null,
                    "assignments"
            ),
            new ConventionRulePreset(
                    "docs(lectures):",
                    "docs",
                    null,
                    "lectures"
            ),
            new ConventionRulePreset(
                    "docs(practice):",
                    "docs",
                    null,
                    "practice"
            ),
            new ConventionRulePreset(
                    "docs(til):",
                    "docs",
                    null,
                    "til"
            ),
            new ConventionRulePreset(
                    "study(java):",
                    "study",
                    "java",
                    null
            ),
            new ConventionRulePreset(
                    "study(spring):",
                    "study",
                    "spring",
                    null
            ),
            new ConventionRulePreset(
                    "study(springboot):",
                    "study",
                    "springboot",
                    null
            ),
            new ConventionRulePreset(
                    "study(kotlin):",
                    "study",
                    "kotlin",
                    null
            ),
            new ConventionRulePreset(
                    "solve(java):",
                    "solve",
                    "java",
                    "codingtest"
            ),
            new ConventionRulePreset(
                    "solve(sql):",
                    "solve",
                    "sql",
                    "codingtest"
            ),
            new ConventionRulePreset(
                    "fix(",
                    "fix",
                    null,
                    null
            ),
            new ConventionRulePreset(
                    "style(",
                    "style",
                    null,
                    null
            ),
            new ConventionRulePreset(
                    "refactor(",
                    "refactor",
                    null,
                    null
            ),
            new ConventionRulePreset(
                    "chore(project):",
                    "chore",
                    "project",
                    "project"
            )
    );

    private DefaultRulePreset() {
    }

    public static boolean supportsDirectory(String directoryName) {
        return DIRECTORY_CATEGORIES.containsKey(directoryName);
    }

    public static String getCategory(String directoryName) {
        return DIRECTORY_CATEGORIES.get(directoryName);
    }

    public static List<ConventionRulePreset> getConventionRules() {
        return CONVENTION_RULES;
    }

    public record ConventionRulePreset(
            String messagePattern,
            String commitType,
            String scope,
            String category
    ) {
    }
}