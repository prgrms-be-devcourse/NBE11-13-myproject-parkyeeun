package com.repoary.backend.github.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RecordCheckBaselineFixture {

    static final Long USER_ID = 1L;
    static final Long CONNECTED_REPOSITORY_ID = 11L;
    static final String OWNER = "dPdms21";
    static final String REPOSITORY_NAME = "programmers-devcourse-be11";
    static final String FULL_NAME = OWNER + "/" + REPOSITORY_NAME;
    static final String DEFAULT_BRANCH = "main";
    static final YearMonth MONTH = YearMonth.of(2026, 9);
    static final String TEST_ACCESS_TOKEN = "baseline-test-token";

    static final List<LocalDate> TARGET_DATES = List.of(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 3),
            LocalDate.of(2026, 9, 4),
            LocalDate.of(2026, 9, 6),
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 9, 8),
            LocalDate.of(2026, 9, 9),
            LocalDate.of(2026, 9, 10),
            LocalDate.of(2026, 9, 21),
            LocalDate.of(2026, 9, 22)
    );

    static final Set<LocalDate> COMPLETE_DATES = Set.copyOf(
            TARGET_DATES.subList(0, 10)
    );

    static final LocalDate MISSING_DATE = LocalDate.of(2026, 9, 22);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private RecordCheckBaselineFixture() {
    }

    static String commitsJson(ObjectMapper objectMapper) {
        List<Map<String, Object>> commits = TARGET_DATES.stream()
                .map(RecordCheckBaselineFixture::commit)
                .toList();
        return writeJson(objectMapper, commits);
    }

    static String readmeJson(ObjectMapper objectMapper) {
        String readme = COMPLETE_DATES.stream()
                .sorted()
                .map(date -> String.format(
                        "| [%s](./%s.md) | Baseline fixture |",
                        date,
                        date
                ))
                .reduce("", (left, right) -> left + right + "\n");

        String encoded = Base64.getEncoder().encodeToString(
                readme.getBytes(StandardCharsets.UTF_8)
        );

        return writeJson(
                objectMapper,
                content(
                        "README.md",
                        "til/2026-09/README.md",
                        encoded,
                        "base64"
                )
        );
    }

    static String tilJson(
            ObjectMapper objectMapper,
            LocalDate date
    ) {
        String fileName = date + ".md";
        return writeJson(
                objectMapper,
                content(
                        fileName,
                        "til/2026-09/" + fileName,
                        null,
                        null
                )
        );
    }

    static String directoryJson(ObjectMapper objectMapper) {
        List<Map<String, Object>> entries = new java.util.ArrayList<>();
        for (LocalDate date : COMPLETE_DATES.stream().sorted().toList()) {
            String fileName = date + ".md";
            entries.add(content(
                    fileName,
                    "til/2026-09/" + fileName,
                    null,
                    null
            ));
        }
        entries.add(content(
                "README.md",
                "til/2026-09/README.md",
                null,
                null
        ));
        return writeJson(objectMapper, entries);
    }

    private static Map<String, Object> commit(LocalDate date) {
        String committedAt = date
                .atTime(15, 0)
                .atZone(KST)
                .toInstant()
                .toString();

        Map<String, Object> committer = new LinkedHashMap<>();
        committer.put("date", committedAt);

        Map<String, Object> commit = new LinkedHashMap<>();
        commit.put(
                "message",
                "study(baseline): " + date + " fixture"
        );
        commit.put("committer", committer);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sha", "fixture-" + date);
        response.put(
                "html_url",
                "https://github.test/" + FULL_NAME + "/commit/fixture-" + date
        );
        response.put("commit", commit);
        return response;
    }

    private static Map<String, Object> content(
            String name,
            String path,
            String content,
            String encoding
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", name);
        response.put("path", path);
        response.put("type", "file");
        response.put("content", content);
        response.put("encoding", encoding);
        return response;
    }

    private static String writeJson(
            ObjectMapper objectMapper,
            Object value
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Baseline fixture JSON을 생성할 수 없습니다.",
                    exception
            );
        }
    }
}
