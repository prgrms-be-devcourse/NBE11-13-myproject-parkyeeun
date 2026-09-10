package com.repoary.backend.readme.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReadmeSummaryGenerator {

    private static final String LEARNING_SUMMARY_HEADING =
            "## 오늘 학습 정리";

    public String generate(String tilContent) {
        if (tilContent == null || tilContent.isBlank()) {
            throw new IllegalArgumentException(
                    "TIL 내용은 필수입니다."
            );
        }

        String section = extractLearningSummarySection(tilContent);

        if (section.isBlank()) {
            throw new IllegalStateException(
                    "TIL의 오늘 학습 정리 내용을 찾을 수 없습니다."
            );
        }

        return parseSummary(section);
    }

    private String extractLearningSummarySection(String tilContent) {
        int startIndex =
                tilContent.indexOf(LEARNING_SUMMARY_HEADING);

        if (startIndex < 0) {
            return "";
        }

        int contentStart =
                startIndex + LEARNING_SUMMARY_HEADING.length();

        int nextHeadingIndex =
                tilContent.indexOf(
                        "\n## ",
                        contentStart
                );

        if (nextHeadingIndex < 0) {
            return tilContent
                    .substring(contentStart)
                    .trim();
        }

        return tilContent
                .substring(
                        contentStart,
                        nextHeadingIndex
                )
                .trim();
    }

    private String parseSummary(String section) {
        List<String> groups = new ArrayList<>();
        List<String> currentItems = new ArrayList<>();

        for (String rawLine : section.split("\\R")) {
            String line = rawLine.trim();

            if (line.isBlank()) {
                continue;
            }

            if (isCategoryLine(line)) {
                addGroup(groups, currentItems);
                continue;
            }

            if (line.startsWith("- ")) {
                String item =
                        removeMarkdownLink(
                                line.substring(2).trim()
                        );

                if (!item.isBlank()) {
                    currentItems.add(item);
                }
            }
        }

        addGroup(groups, currentItems);

        if (groups.isEmpty()) {
            throw new IllegalStateException(
                    "README Summary로 변환할 학습 내용이 없습니다."
            );
        }

        return String.join(" / ", groups);
    }

    private boolean isCategoryLine(String line) {
        return line.startsWith("**")
                && line.endsWith("**")
                && line.length() > 4;
    }

    private void addGroup(
            List<String> groups,
            List<String> currentItems
    ) {
        if (currentItems.isEmpty()) {
            return;
        }

        groups.add(
                String.join(" · ", currentItems)
        );

        currentItems.clear();
    }

    private String removeMarkdownLink(String item) {
        return item
                .replaceAll(
                        "\\s*\\[🔗[^]]*]\\([^)]*\\)",
                        ""
                )
                .trim();
    }
}