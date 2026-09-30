package com.repoary.backend.analysis.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConventionalCommitParser {

    private static final Pattern CONVENTIONAL_COMMIT_PATTERN =
            Pattern.compile(
                    "^([A-Za-z]+)(?:\\(([^)]+)\\))?:\\s+(.+)$"
            );

    public ParsedCommitMessage parse(String message) {
        String firstLine = getFirstLine(message);
        Matcher matcher =
                CONVENTIONAL_COMMIT_PATTERN.matcher(firstLine);

        if (!matcher.matches()) {
            return new ParsedCommitMessage(null, null);
        }

        return new ParsedCommitMessage(
                matcher.group(1),
                matcher.group(2)
        );
    }

    private String getFirstLine(String message) {
        if (message == null) {
            return "";
        }

        return message.lines()
                .findFirst()
                .orElse("")
                .trim();
    }

    public record ParsedCommitMessage(
            String commitType,
            String scope
    ) {
    }
}
