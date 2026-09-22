package com.repoary.backend.github.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repoary.backend.recordcheck.controller.RecordCheckController;
import com.repoary.backend.recordcheck.service.RecordCheckService;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.repoary.backend.github.client.RecordCheckBaselineFixture.CONNECTED_REPOSITORY_ID;
import static com.repoary.backend.github.client.RecordCheckBaselineFixture.DEFAULT_BRANCH;
import static com.repoary.backend.github.client.RecordCheckBaselineFixture.FULL_NAME;
import static com.repoary.backend.github.client.RecordCheckBaselineFixture.MONTH;
import static com.repoary.backend.github.client.RecordCheckBaselineFixture.TARGET_DATES;
import static com.repoary.backend.github.client.RecordCheckBaselineFixture.TEST_ACCESS_TOKEN;
import static com.repoary.backend.github.client.RecordCheckBaselineFixture.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class RecordCheckPostOptimizationPerformanceTest {

    private static final String MOCK_GITHUB_BASE_URL =
            "https://api.github.test";
    private static final int WARMUP_COUNT = 1;
    private static final int MEASUREMENT_COUNT = 10;
    private static final int TOTAL_INVOCATIONS =
            WARMUP_COUNT + MEASUREMENT_COUNT;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("월 디렉터리 최적화 후 Mock 호출 수와 응답 시간을 측정한다")
    void measuresPostOptimizationPerformance() throws Exception {
        JsonNode expectedResponse = readExpectedResponse();
        ScenarioResult noDelay = measureScenario(
                "mock-no-delay",
                0,
                expectedResponse
        );
        ScenarioResult syntheticDelay = measureScenario(
                "mock-fixed-20ms-per-github-request",
                20,
                expectedResponse
        );

        Path reportPath = writeReport(
                expectedResponse,
                List.of(noDelay, syntheticDelay)
        );
        printResult(noDelay);
        printResult(syntheticDelay);
        System.out.println("postOptimizationReport="
                + reportPath.toAbsolutePath());
    }

    private ScenarioResult measureScenario(
            String name,
            int mockDelayMs,
            JsonNode expectedResponse
    ) throws Exception {
        Harness harness = createHarness(mockDelayMs);

        Measurement warmup = executeOnce(harness, expectedResponse);
        assertExpectedCalls(warmup.calls());

        List<Double> durationsMs = new ArrayList<>();
        List<CallSnapshot> calls = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            try {
                Measurement measurement = executeOnce(
                        harness,
                        expectedResponse
                );
                assertExpectedCalls(measurement.calls());
                durationsMs.add(measurement.durationMs());
                calls.add(measurement.calls());
                successCount++;
            } catch (Exception | AssertionError exception) {
                failureCount++;
                throw exception;
            }
        }

        harness.server().verify();
        assertThat(successCount).isEqualTo(MEASUREMENT_COUNT);
        assertThat(failureCount).isZero();
        assertThat(calls).allSatisfy(this::assertExpectedCalls);

        List<Double> sorted = durationsMs.stream().sorted().toList();
        double average = durationsMs.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();
        int p95Index = (int) Math.ceil(0.95 * sorted.size()) - 1;

        return new ScenarioResult(
                name,
                mockDelayMs,
                WARMUP_COUNT,
                MEASUREMENT_COUNT,
                durationsMs,
                average,
                sorted.get(p95Index),
                sorted.get(0),
                sorted.get(sorted.size() - 1),
                successCount,
                failureCount,
                calls.get(0)
        );
    }

    private Harness createHarness(int mockDelayMs) {
        RequestCounter counter = new RequestCounter();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(MOCK_GITHUB_BASE_URL)
                .requestInterceptor((request, body, execution) -> {
                    counter.record(request.getURI());
                    return execution.execute(request, body);
                });
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .ignoreExpectOrder(true)
                .build();
        configureGitHubResponses(server, mockDelayMs);

        UserRepository userRepository = mock(UserRepository.class);
        ConnectedRepositoryRepository repositoryRepository =
                mock(ConnectedRepositoryRepository.class);
        User user = mock(User.class);
        ConnectedRepository repository = mock(ConnectedRepository.class);

        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(user));
        when(repositoryRepository.findByIdAndUser(
                CONNECTED_REPOSITORY_ID,
                user
        )).thenReturn(java.util.Optional.of(repository));
        when(user.getGithubAccessToken()).thenReturn(TEST_ACCESS_TOKEN);
        when(repository.getFullName()).thenReturn(FULL_NAME);
        when(repository.getDefaultBranch()).thenReturn(DEFAULT_BRANCH);

        RecordCheckService service = new RecordCheckService(
                userRepository,
                repositoryRepository,
                new GitHubApiClient(builder)
        );
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RecordCheckController(service))
                .build();
        return new Harness(mockMvc, server, counter);
    }

    private void configureGitHubResponses(
            MockRestServiceServer server,
            int mockDelayMs
    ) {
        String repositoryPath =
                "/repos/dPdms21/programmers-devcourse-be11";
        String commitsUrl = MOCK_GITHUB_BASE_URL
                + repositoryPath
                + "/commits?sha=main"
                + "&since=2026-08-24T21:00:00Z"
                + "&until=2026-10-07T21:00:00Z"
                + "&per_page=100&page=1";
        String readmeUrl = MOCK_GITHUB_BASE_URL
                + repositoryPath
                + "/contents/til/2026-09/README.md?ref=main";
        String directoryUrl = MOCK_GITHUB_BASE_URL
                + repositoryPath
                + "/contents/til/2026-09?ref=main";

        server.expect(
                        ExpectedCount.times(TOTAL_INVOCATIONS),
                        requestTo(commitsUrl)
                )
                .andRespond(delayed(
                        withSuccess(
                                RecordCheckBaselineFixture.commitsJson(
                                        objectMapper
                                ),
                                MediaType.APPLICATION_JSON
                        ),
                        mockDelayMs
                ));
        server.expect(
                        ExpectedCount.times(TOTAL_INVOCATIONS),
                        requestTo(readmeUrl)
                )
                .andRespond(delayed(
                        withSuccess(
                                RecordCheckBaselineFixture.readmeJson(
                                        objectMapper
                                ),
                                MediaType.APPLICATION_JSON
                        ),
                        mockDelayMs
                ));
        server.expect(
                        ExpectedCount.times(TOTAL_INVOCATIONS),
                        requestTo(directoryUrl)
                )
                .andRespond(delayed(
                        withSuccess(
                                RecordCheckBaselineFixture.directoryJson(
                                        objectMapper
                                ),
                                MediaType.APPLICATION_JSON
                        ),
                        mockDelayMs
                ));
    }

    private ResponseCreator delayed(
            ResponseCreator delegate,
            int delayMs
    ) {
        return request -> {
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "Mock GitHub 지연 대기가 중단되었습니다.",
                            exception
                    );
                }
            }
            return delegate.createResponse(request);
        };
    }

    private Measurement executeOnce(
            Harness harness,
            JsonNode expectedResponse
    ) throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                USER_ID,
                null,
                List.of()
        );
        long startedAt = System.nanoTime();
        MvcResult result = harness.mockMvc()
                .perform(get(
                                "/api/repositories/{connectedRepositoryId}/record-checks",
                                CONNECTED_REPOSITORY_ID
                        )
                                .principal(authentication)
                                .queryParam("month", MONTH.toString()))
                .andReturn();
        long finishedAt = System.nanoTime();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode actual = objectMapper.readTree(
                result.getResponse().getContentAsByteArray()
        );
        assertThat(actual).isEqualTo(expectedResponse);
        return new Measurement(
                (finishedAt - startedAt) / 1_000_000.0,
                harness.counter().drain()
        );
    }

    private void assertExpectedCalls(CallSnapshot calls) {
        assertThat(calls.commitCalls()).isEqualTo(1);
        assertThat(calls.readmeCalls()).isEqualTo(1);
        assertThat(calls.directoryCalls()).isEqualTo(1);
        assertThat(calls.tilCalls()).isZero();
        assertThat(calls.unexpectedCalls()).isZero();
        assertThat(calls.totalCalls()).isEqualTo(3);
        assertThat(calls.commitPages()).containsExactly(1);
    }

    private JsonNode readExpectedResponse() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/recordcheck/baseline-2026-09-expected.json"
        )) {
            if (input == null) {
                throw new IOException("Baseline expected JSON이 없습니다.");
            }
            return objectMapper.readTree(input);
        }
    }

    private Path writeReport(
            JsonNode expectedResponse,
            List<ScenarioResult> scenarios
    ) throws IOException {
        Path directory = Path.of(
                "build",
                "reports",
                "record-check-baseline"
        );
        Files.createDirectories(directory);
        ZonedDateTime now = ZonedDateTime.now(
                ZoneId.of("Asia/Seoul")
        );
        String timestamp = now.format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
        );
        Path reportPath = directory.resolve(
                "post-optimization-" + timestamp + ".json"
        );

        Map<String, Object> report = new LinkedHashMap<>();
        report.put(
                "measurementType",
                "POST_OPTIMIZATION_MOCK_MVC_WITH_MOCK_GITHUB_HTTP"
        );
        report.put("measuredAtKst", now.toString());
        report.put("environment", "local-test-jvm");
        report.put("javaVersion", System.getProperty("java.version"));
        report.put("repository", FULL_NAME);
        report.put("connectedRepositoryId", CONNECTED_REPOSITORY_ID);
        report.put("branch", DEFAULT_BRANCH);
        report.put("month", MONTH.toString());
        report.put("githubRealCalls", false);
        report.put("databaseRealCalls", false);
        report.put(
                "commitFixtureType",
                "SEMANTIC_TARGET_DATE_FIXTURE_NOT_FULL_GITHUB_COMMIT_SNAPSHOT"
        );
        report.put("targetDateCount", TARGET_DATES.size());
        report.put("expectedResponse", expectedResponse);
        report.put("scenarios", scenarios);
        report.put(
                "limitations",
                List.of(
                        "실제 GitHub 및 네트워크 응답 시간이 아니다.",
                        "Mock Repository를 사용하므로 실제 DB 지연을 포함하지 않는다.",
                        "20ms는 호출 수 차이를 비교하기 위한 합성 지연이다.",
                        "Thread.sleep 기반 합성 지연은 OS 스케줄링에 따라 20ms를 초과할 수 있다.",
                        "표본 10개의 nearest-rank p95는 최댓값과 같다."
                )
        );

        try (var output = Files.newOutputStream(
                reportPath,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        )) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(output, report);
        }
        return reportPath;
    }

    private void printResult(ScenarioResult result) {
        System.out.printf(
                Locale.ROOT,
                "%s calls=%d/%d/%d/%d/%d averageMs=%.3f p95Ms=%.3f minMs=%.3f maxMs=%.3f success=%d failure=%d%n",
                result.name(),
                result.callsPerRequest().commitCalls(),
                result.callsPerRequest().readmeCalls(),
                result.callsPerRequest().directoryCalls(),
                result.callsPerRequest().tilCalls(),
                result.callsPerRequest().totalCalls(),
                result.averageMs(),
                result.p95Ms(),
                result.minMs(),
                result.maxMs(),
                result.successCount(),
                result.failureCount()
        );
    }

    private record Harness(
            MockMvc mockMvc,
            MockRestServiceServer server,
            RequestCounter counter
    ) {
    }

    private record Measurement(
            double durationMs,
            CallSnapshot calls
    ) {
    }

    private record ScenarioResult(
            String name,
            int mockGitHubDelayMs,
            int warmupCount,
            int measurementCount,
            List<Double> individualDurationsMs,
            double averageMs,
            double p95Ms,
            double minMs,
            double maxMs,
            int successCount,
            int failureCount,
            CallSnapshot callsPerRequest
    ) {
    }

    private record CallSnapshot(
            int commitCalls,
            int readmeCalls,
            int directoryCalls,
            int tilCalls,
            int unexpectedCalls,
            int totalCalls,
            List<Integer> commitPages
    ) {
    }

    private static final class RequestCounter {

        private int commitCalls;
        private int readmeCalls;
        private int directoryCalls;
        private int tilCalls;
        private int unexpectedCalls;
        private final List<Integer> commitPages = new ArrayList<>();

        synchronized void record(URI uri) {
            String path = uri.getPath();
            String query = uri.getQuery();
            if (path.endsWith("/commits")) {
                commitCalls++;
                commitPages.add(readPage(query));
            } else if (path.endsWith(
                    "/contents/til/2026-09/README.md"
            )) {
                readmeCalls++;
            } else if (path.endsWith("/contents/til/2026-09")) {
                directoryCalls++;
            } else if (path.matches(
                    ".*/contents/til/2026-09/2026-09-\\d{2}\\.md"
            )) {
                tilCalls++;
            } else {
                unexpectedCalls++;
            }
        }

        synchronized CallSnapshot drain() {
            int total = commitCalls
                    + readmeCalls
                    + directoryCalls
                    + tilCalls
                    + unexpectedCalls;
            CallSnapshot snapshot = new CallSnapshot(
                    commitCalls,
                    readmeCalls,
                    directoryCalls,
                    tilCalls,
                    unexpectedCalls,
                    total,
                    List.copyOf(commitPages)
            );
            commitCalls = 0;
            readmeCalls = 0;
            directoryCalls = 0;
            tilCalls = 0;
            unexpectedCalls = 0;
            commitPages.clear();
            return snapshot;
        }

        private int readPage(String query) {
            if (query == null) {
                return -1;
            }
            for (String parameter : query.split("&")) {
                String[] pair = parameter.split("=", 2);
                if (pair.length == 2 && "page".equals(pair[0])) {
                    return Integer.parseInt(pair[1]);
                }
            }
            return -1;
        }
    }
}
