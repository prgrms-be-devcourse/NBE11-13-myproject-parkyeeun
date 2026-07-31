package com.repoary.backend.analysis.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class AnalysisFileFilter {

    private static final Set<String> INCLUDED_EXTENSIONS = Set.of(
            ".java",
            ".kt",
            ".kts",
            ".sql",
            ".md",
            ".yml",
            ".yaml",
            ".properties",
            ".gradle",
            ".html",
            ".css",
            ".js",
            ".jsx",
            ".ts",
            ".tsx",
            ".json",
            ".xml"
    );

    private static final Set<String> EXCLUDED_FILE_NAMES = Set.of(
            ".gitattributes",
            ".gitignore",
            "gradlew",
            "gradlew.bat",
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml"
    );

    private static final Set<String> EXCLUDED_PATH_SEGMENTS = Set.of(
            ".gradle",
            "build",
            "node_modules",
            "dist",
            "target"
    );

    public boolean isRelevant(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        String normalizedPath = normalize(filename);
        String fileName = extractFileName(normalizedPath);

        if (EXCLUDED_FILE_NAMES.contains(fileName)) {
            return false;
        }

        if (containsExcludedPathSegment(normalizedPath)) {
            return false;
        }

        return INCLUDED_EXTENSIONS.stream()
                .anyMatch(normalizedPath::endsWith);
    }

    private boolean containsExcludedPathSegment(String normalizedPath) {
        String[] segments = normalizedPath.split("/");

        for (String segment : segments) {
            if (EXCLUDED_PATH_SEGMENTS.contains(segment)) {
                return true;
            }
        }

        return false;
    }

    private String extractFileName(String normalizedPath) {
        int separatorIndex = normalizedPath.lastIndexOf('/');

        if (separatorIndex < 0) {
            return normalizedPath;
        }

        return normalizedPath.substring(separatorIndex + 1);
    }

    private String normalize(String filename) {
        return filename
                .trim()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT);
    }
}