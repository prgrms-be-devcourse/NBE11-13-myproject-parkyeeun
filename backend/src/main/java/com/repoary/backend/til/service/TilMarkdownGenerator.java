package com.repoary.backend.til.service;

import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.repository.domain.ConnectedRepository;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class TilMarkdownGenerator {

    private static final List<String> SUPPORTED_CATEGORIES = List.of(
            "lectures",
            "practice",
            "assignments",
            "codingtest"
    );

    private static final Pattern DATE_PREFIX_PATTERN =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s+");

    public String generate(
            StoredAnalysisResult analysisResult,
            ConnectedRepository connectedRepository
    ) {
        if (analysisResult == null) {
            throw new IllegalArgumentException(
                    "분석 결과는 필수입니다."
            );
        }

        if (connectedRepository == null) {
            throw new IllegalArgumentException(
                    "연결 저장소는 필수입니다."
            );
        }

        List<StoredAnalysisResult.StoredCommitAnalysis> sortedCommits =
                analysisResult.commits().stream()
                        .sorted(
                                Comparator.comparing(
                                        StoredAnalysisResult.StoredCommitAnalysis::committedAt
                                )
                        )
                        .toList();

        Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
                commitsByCategory =
                classifyCommits(sortedCommits);

        return buildMarkdown(
                analysisResult,
                connectedRepository,
                commitsByCategory
        );
    }

    private Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
    classifyCommits(
            List<StoredAnalysisResult.StoredCommitAnalysis> commits
    ) {
        Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
                classified = new LinkedHashMap<>();

        for (String category : SUPPORTED_CATEGORIES) {
            classified.put(category, new ArrayList<>());
        }

        for (StoredAnalysisResult.StoredCommitAnalysis commit : commits) {
            String category = resolveCategory(commit);

            if (category != null) {
                classified.get(category).add(commit);
            }
        }

        return classified;
    }

    private String resolveCategory(
            StoredAnalysisResult.StoredCommitAnalysis commit
    ) {
        for (String category : commit.categories()) {
            if (SUPPORTED_CATEGORIES.contains(category)) {
                return category;
            }
        }

        return null;
    }

    private String buildMarkdown(
            StoredAnalysisResult analysisResult,
            ConnectedRepository connectedRepository,
            Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
                    commitsByCategory
    ) {
        StringBuilder markdown = new StringBuilder();

        markdown.append("# ")
                .append(analysisResult.targetDate())
                .append(" TIL (Today I Learned)\n\n");

        appendLearningSummary(
                markdown,
                connectedRepository,
                commitsByCategory
        );

        appendLearnedContent(
                markdown,
                commitsByCategory
        );

        appendPracticeAndAssignments(
                markdown,
                commitsByCategory
        );

        markdown.append("## 헷갈렸던 점\n\n");
        markdown.append("## 오늘 느낀 점\n\n");
        markdown.append("## 추가 학습 예정\n");

        return markdown.toString();
    }

    private void appendLearningSummary(
            StringBuilder markdown,
            ConnectedRepository connectedRepository,
            Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
                    commitsByCategory
    ) {
        markdown.append("## 오늘 학습 정리\n\n");

        for (String category : SUPPORTED_CATEGORIES) {
            List<StoredAnalysisResult.StoredCommitAnalysis> commits =
                    commitsByCategory.get(category);

            if (commits == null || commits.isEmpty()) {
                continue;
            }

            markdown.append("**")
                    .append(category)
                    .append("**\n\n");

            for (StoredAnalysisResult.StoredCommitAnalysis commit : commits) {
                markdown.append("- ")
                        .append(extractSummary(commit));

                TilLink link = resolveLink(
                        connectedRepository,
                        category,
                        commit
                );

                if (link != null) {
                    markdown.append(" [🔗 ")
                            .append(link.label())
                            .append("](")
                            .append(link.url())
                            .append(")");
                }

                markdown.append("\n");
            }

            markdown.append("\n");
        }
    }

    private void appendLearnedContent(
            StringBuilder markdown,
            Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
                    commitsByCategory
    ) {
        markdown.append("## 오늘 배운 내용\n\n");

        List<String> summaries = SUPPORTED_CATEGORIES.stream()
                .flatMap(category ->
                        commitsByCategory.get(category).stream()
                )
                .map(this::extractSummary)
                .distinct()
                .toList();

        for (String summary : summaries) {
            markdown.append(summary)
                    .append(" 관련 내용을 학습했다.")
                    .append("\n\n");
        }
    }

    private void appendPracticeAndAssignments(
            StringBuilder markdown,
            Map<String, List<StoredAnalysisResult.StoredCommitAnalysis>>
                    commitsByCategory
    ) {
        markdown.append("## 실습 및 과제\n\n");

        appendPracticeOrAssignment(
                markdown,
                commitsByCategory.get("practice")
        );

        appendPracticeOrAssignment(
                markdown,
                commitsByCategory.get("assignments")
        );
    }

    private void appendPracticeOrAssignment(
            StringBuilder markdown,
            List<StoredAnalysisResult.StoredCommitAnalysis> commits
    ) {
        if (commits == null || commits.isEmpty()) {
            return;
        }

        for (StoredAnalysisResult.StoredCommitAnalysis commit : commits) {
            markdown.append("> ")
                    .append(extractSummary(commit))
                    .append("\n\n");
        }
    }

    private String extractSummary(
            StoredAnalysisResult.StoredCommitAnalysis commit
    ) {
        String message = commit.message();

        if (message == null || message.isBlank()) {
            return "학습 내용";
        }

        int colonIndex = message.indexOf(':');

        String summary;

        if (colonIndex >= 0
                && colonIndex + 1 < message.length()) {
            summary = message.substring(colonIndex + 1).trim();
        } else {
            summary = message.trim();
        }

        return DATE_PREFIX_PATTERN
                .matcher(summary)
                .replaceFirst("")
                .trim();
    }

    private TilLink resolveLink(
            ConnectedRepository connectedRepository,
            String category,
            StoredAnalysisResult.StoredCommitAnalysis commit
    ) {
        List<String> filenames = commit.files().stream()
                .filter(file ->
                        category.equals(file.category())
                )
                .map(StoredAnalysisResult.StoredFileAnalysis::filename)
                .filter(filename ->
                        filename != null && !filename.isBlank()
                )
                .toList();

        if (filenames.isEmpty()) {
            return null;
        }

        if ("codingtest".equals(category)
                && filenames.size() == 1) {
            return createFileLink(
                    connectedRepository,
                    filenames.get(0)
            );
        }

        String learningUnitPath =
                resolveLearningUnitPath(
                        category,
                        filenames.get(0)
                );

        if (learningUnitPath == null) {
            return null;
        }

        return createDirectoryLink(
                connectedRepository,
                learningUnitPath
        );
    }

    private String resolveLearningUnitPath(
            String category,
            String filename
    ) {
        String[] parts = filename.split("/");

        if (parts.length == 0) {
            return null;
        }

        if ("practice".equals(category)
                || "lectures".equals(category)) {
            if (parts.length >= 2) {
                return parts[0] + "/" + parts[1];
            }

            return parts[0];
        }

        if ("assignments".equals(category)) {
            return resolveAssignmentPath(parts);
        }

        if ("codingtest".equals(category)) {
            return resolveParentPath(filename);
        }

        return null;
    }

    private String resolveAssignmentPath(String[] parts) {
        /*
         * assignments/src/main/java/vendingmachine/... 구조라면
         * 과제 패키지 단위까지 링크한다.
         */
        if (parts.length >= 5
                && "assignments".equals(parts[0])
                && "src".equals(parts[1])
                && "main".equals(parts[2])
                && "java".equals(parts[3])) {
            return String.join(
                    "/",
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4]
            );
        }

        if (parts.length >= 2) {
            return parts[0] + "/" + parts[1];
        }

        return parts[0];
    }

    private String resolveParentPath(String filename) {
        int lastSlashIndex = filename.lastIndexOf('/');

        if (lastSlashIndex < 0) {
            return null;
        }

        return filename.substring(0, lastSlashIndex);
    }

    private TilLink createFileLink(
            ConnectedRepository connectedRepository,
            String filename
    ) {
        String label = getFileNameWithoutExtension(filename);

        String url = githubBaseUrl(connectedRepository)
                + "/blob/"
                + encodePath(connectedRepository.getDefaultBranch())
                + "/"
                + encodePath(filename);

        return new TilLink(label, url);
    }

    private TilLink createDirectoryLink(
            ConnectedRepository connectedRepository,
            String directoryPath
    ) {
        String label = getLastPathSegment(directoryPath);

        String url = githubBaseUrl(connectedRepository)
                + "/tree/"
                + encodePath(connectedRepository.getDefaultBranch())
                + "/"
                + encodePath(directoryPath);

        return new TilLink(label, url);
    }

    private String githubBaseUrl(
            ConnectedRepository connectedRepository
    ) {
        return "https://github.com/"
                + connectedRepository.getFullName();
    }

    private String getFileNameWithoutExtension(String filename) {
        String fileName = Path.of(filename)
                .getFileName()
                .toString();

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0) {
            return fileName;
        }

        return fileName.substring(0, dotIndex);
    }

    private String getLastPathSegment(String path) {
        int lastSlashIndex = path.lastIndexOf('/');

        if (lastSlashIndex < 0) {
            return path;
        }

        return path.substring(lastSlashIndex + 1);
    }

    private String encodePath(String path) {
        return List.of(path.split("/"))
                .stream()
                .map(segment ->
                        URLEncoder.encode(
                                segment,
                                StandardCharsets.UTF_8
                        ).replace("+", "%20")
                )
                .reduce((left, right) ->
                        left + "/" + right
                )
                .orElse("");
    }

    private record TilLink(
            String label,
            String url
    ) {
    }
}