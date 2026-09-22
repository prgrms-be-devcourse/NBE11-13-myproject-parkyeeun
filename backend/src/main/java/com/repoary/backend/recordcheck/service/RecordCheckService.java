package com.repoary.backend.recordcheck.service;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.github.exception.GitHubErrorCode;
import com.repoary.backend.recordcheck.exception.RecordCheckErrorCode;
import com.repoary.backend.repository.exception.RepositoryErrorCode;
import com.repoary.backend.user.exception.UserErrorCode;
import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.github.dto.GitHubCommitResponse;
import com.repoary.backend.github.dto.GitHubContentResponse;
import com.repoary.backend.recordcheck.dto.RecordCheckItemResponse;
import com.repoary.backend.recordcheck.dto.RecordCheckResponse;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecordCheckService {

    private static final ZoneId KST =
            ZoneId.of("Asia/Seoul");

    private static final LocalTime DAY_BOUNDARY =
            LocalTime.of(6, 0);

    private static final String TIL_COMMIT_PREFIX =
            "docs(til):";

    private static final Pattern COMMIT_DATE_PATTERN =
            Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");

    private static final int GITHUB_DIRECTORY_ITEM_LIMIT = 1_000;

    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final GitHubApiClient gitHubApiClient;

    public RecordCheckService(
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            GitHubApiClient gitHubApiClient
    ) {
        this.userRepository = userRepository;
        this.connectedRepositoryRepository =
                connectedRepositoryRepository;
        this.gitHubApiClient = gitHubApiClient;
    }

    @Transactional(readOnly = true)
    public RecordCheckResponse check(
            Long userId,
            Long connectedRepositoryId,
            YearMonth month
    ) {
        if (month == null) {
            throw new BusinessException(RecordCheckErrorCode.MONTH_REQUIRED);
        }

        RepositoryContext context =
                getRepositoryContext(
                        userId,
                        connectedRepositoryId
                );

        List<LocalDate> targetDates =
                getCommitDates(
                        context,
                        month
                );

        if (targetDates.isEmpty()) {
            return new RecordCheckResponse(
                    month,
                    List.of()
            );
        }

        String readmeContent =
                getMonthlyReadmeContent(
                        context,
                        month
                );

        Set<LocalDate> existingTilDates =
                getExistingTilDates(
                        context,
                        month,
                        targetDates
                );

        List<RecordCheckItemResponse> items =
                targetDates.stream()
                        .map(targetDate ->
                                createItem(
                                        targetDate,
                                        readmeContent,
                                        existingTilDates
                                )
                        )
                        .toList();

        return new RecordCheckResponse(
                month,
                items
        );
    }

    private List<LocalDate> getCommitDates(
            RepositoryContext context,
            YearMonth month
    ) {
        LocalDate monthStart =
                month.atDay(1);

        LocalDate nextMonthStart =
                month.plusMonths(1).atDay(1);

        // 커밋 메시지의 학습 날짜와 실제 GitHub 커밋 시각이
        // 며칠 차이날 수 있으므로 월 경계에 여유 기간을 둔다.
        LocalDate queryStart =
                monthStart.minusDays(7);

        LocalDate queryEnd =
                nextMonthStart.plusDays(7);

        Instant since =
                queryStart
                        .atTime(DAY_BOUNDARY)
                        .atZone(KST)
                        .toInstant();

        Instant until =
                queryEnd
                        .atTime(DAY_BOUNDARY)
                        .atZone(KST)
                        .toInstant();

        List<GitHubCommitResponse> commits =
                gitHubApiClient.getCommits(
                        context.user().getGithubAccessToken(),
                        context.owner(),
                        context.repositoryName(),
                        context.repository().getDefaultBranch(),
                        since,
                        until
                );

        return commits.stream()
                .filter(this::isLearningCommit)
                .map(this::getLearningDate)
                .filter(date ->
                        YearMonth.from(date).equals(month)
                )
                .distinct()
                .sorted()
                .toList();
    }

    private boolean isLearningCommit(
            GitHubCommitResponse commit
    ) {
        String message = commit.message();

        if (message == null || message.isBlank()) {
            return false;
        }

        return !message
                .trim()
                .startsWith(TIL_COMMIT_PREFIX);
    }

    private LocalDate getLearningDate(
            GitHubCommitResponse commit
    ) {
        LocalDate messageDate =
                extractDateFromCommitMessage(
                        commit.message()
                );

        if (messageDate != null) {
            return messageDate;
        }

        return toLearningDate(
                commit.committedAt()
        );
    }

    private LocalDate extractDateFromCommitMessage(
            String message
    ) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher =
                COMMIT_DATE_PATTERN.matcher(message);

        if (!matcher.find()) {
            return null;
        }

        try {
            return LocalDate.parse(
                    matcher.group(1)
            );
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDate toLearningDate(
            Instant committedAt
    ) {
        ZonedDateTime committedAtKst =
                committedAt.atZone(KST);

        LocalDate date =
                committedAtKst.toLocalDate();

        if (committedAtKst
                .toLocalTime()
                .isBefore(DAY_BOUNDARY)) {
            return date.minusDays(1);
        }

        return date;
    }

    private RecordCheckItemResponse createItem(
            LocalDate targetDate,
            String readmeContent,
            Set<LocalDate> existingTilDates
    ) {
        boolean tilExists =
                existingTilDates.contains(targetDate);

        boolean readmeEntryExists =
                containsReadmeEntry(
                        readmeContent,
                        targetDate
                );

        return new RecordCheckItemResponse(
                targetDate,
                tilExists,
                readmeEntryExists
        );
    }

    private Set<LocalDate> getExistingTilDates(
            RepositoryContext context,
            YearMonth month,
            List<LocalDate> targetDates
    ) {
        String directoryPath = "til/" + month;

        Optional<List<GitHubContentResponse>> directoryContents =
                gitHubApiClient.getDirectoryContents(
                        context.user().getGithubAccessToken(),
                        context.owner(),
                        context.repositoryName(),
                        context.repository().getDefaultBranch(),
                        directoryPath
                );

        if (directoryContents.isEmpty()) {
            return Set.of();
        }

        List<GitHubContentResponse> entries = directoryContents.get();
        if (requiresPerFileFallback(entries, month, targetDates)) {
            return getExistingTilDatesByFile(
                    context,
                    targetDates
            );
        }

        Map<String, LocalDate> expectedDatesByPath =
                expectedDatesByPath(month, targetDates);
        Set<LocalDate> existingDates = new HashSet<>();

        for (GitHubContentResponse entry : entries) {
            LocalDate targetDate = expectedDatesByPath.get(entry.path());
            if (targetDate != null) {
                existingDates.add(targetDate);
            }
        }

        return Set.copyOf(existingDates);
    }

    private boolean requiresPerFileFallback(
            List<GitHubContentResponse> entries,
            YearMonth month,
            List<LocalDate> targetDates
    ) {
        if (entries.size() >= GITHUB_DIRECTORY_ITEM_LIMIT) {
            return true;
        }

        Map<String, LocalDate> expectedDatesByPath =
                expectedDatesByPath(month, targetDates);
        Set<String> expectedNames = new HashSet<>();
        expectedDatesByPath.keySet().forEach(path ->
                expectedNames.add(path.substring(path.lastIndexOf('/') + 1))
        );
        Set<String> matchedPaths = new HashSet<>();

        for (GitHubContentResponse entry : entries) {
            if (entry == null
                    || entry.name() == null
                    || entry.name().isBlank()
                    || entry.path() == null
                    || entry.path().isBlank()
                    || entry.type() == null
                    || entry.type().isBlank()) {
                return true;
            }

            boolean expectedName = expectedNames.contains(entry.name());
            boolean expectedPath = expectedDatesByPath.containsKey(entry.path());

            if (expectedName != expectedPath) {
                return true;
            }

            if (expectedPath) {
                if (!"file".equals(entry.type())
                        || !entry.path().endsWith("/" + entry.name())
                        || !matchedPaths.add(entry.path())) {
                    return true;
                }
            }
        }

        return false;
    }

    private Map<String, LocalDate> expectedDatesByPath(
            YearMonth month,
            List<LocalDate> targetDates
    ) {
        Map<String, LocalDate> datesByPath = new HashMap<>();
        for (LocalDate targetDate : targetDates) {
            datesByPath.put(
                    String.format(
                            "til/%s/%s.md",
                            month,
                            targetDate
                    ),
                    targetDate
            );
        }
        return datesByPath;
    }

    private Set<LocalDate> getExistingTilDatesByFile(
            RepositoryContext context,
            List<LocalDate> targetDates
    ) {
        Set<LocalDate> existingDates = new HashSet<>();
        for (LocalDate targetDate : targetDates) {
            if (existsTilFile(context, targetDate)) {
                existingDates.add(targetDate);
            }
        }
        return Set.copyOf(existingDates);
    }

    private boolean existsTilFile(
            RepositoryContext context,
            LocalDate targetDate
    ) {
        String month =
                YearMonth.from(targetDate)
                        .toString();

        String path =
                String.format(
                        "til/%s/%s.md",
                        month,
                        targetDate
                );

        return gitHubApiClient
                .getContent(
                        context.user()
                                .getGithubAccessToken(),
                        context.owner(),
                        context.repositoryName(),
                        context.repository()
                                .getDefaultBranch(),
                        path
                )
                .isPresent();
    }

    private String getMonthlyReadmeContent(
            RepositoryContext context,
            YearMonth month
    ) {
        String path =
                String.format(
                        "til/%s/README.md",
                        month
                );

        Optional<GitHubContentResponse> content =
                gitHubApiClient.getContent(
                        context.user()
                                .getGithubAccessToken(),
                        context.owner(),
                        context.repositoryName(),
                        context.repository()
                                .getDefaultBranch(),
                        path
                );

        if (content.isEmpty()) {
            return "";
        }

        return decodeContent(
                content.get()
        );
    }

    private String decodeContent(
            GitHubContentResponse content
    ) {
        if (content.content() == null
                || content.content().isBlank()) {
            return "";
        }

        if (!"base64".equalsIgnoreCase(
                content.encoding()
        )) {
            throw new ExternalSystemException(
                    GitHubErrorCode.INVALID_RESPONSE,
                    null
            );
        }

        try {
            byte[] decoded =
                    Base64.getMimeDecoder()
                            .decode(
                                    content.content()
                            );

            return new String(
                    decoded,
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException exception) {
            throw new ExternalSystemException(
                    GitHubErrorCode.INVALID_RESPONSE,
                    exception
            );
        }
    }

    private boolean containsReadmeEntry(
            String readmeContent,
            LocalDate targetDate
    ) {
        if (readmeContent == null
                || readmeContent.isBlank()) {
            return false;
        }

        String expectedLink =
                String.format(
                        "[%s](./%s.md)",
                        targetDate,
                        targetDate
                );

        return readmeContent.contains(
                expectedLink
        );
    }

    private RepositoryContext getRepositoryContext(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        UserErrorCode.USER_NOT_FOUND
                                )
                        );

        ConnectedRepository repository =
                connectedRepositoryRepository
                        .findByIdAndUser(
                                connectedRepositoryId,
                                user
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        RepositoryErrorCode.CONNECTED_REPOSITORY_NOT_FOUND
                                )
                        );

        if (user.getGithubAccessToken() == null
                || user.getGithubAccessToken()
                .isBlank()) {
            throw new BusinessException(GitHubErrorCode.ACCESS_TOKEN_MISSING);
        }

        String[] repositoryName =
                repository.getFullName()
                        .split("/", 2);

        if (repositoryName.length != 2
                || repositoryName[0].isBlank()
                || repositoryName[1].isBlank()) {
            throw new IllegalStateException(
                    "GitHub 저장소 이름 형식이 올바르지 않습니다."
            );
        }

        return new RepositoryContext(
                user,
                repository,
                repositoryName[0],
                repositoryName[1]
        );
    }

    private record RepositoryContext(
            User user,
            ConnectedRepository repository,
            String owner,
            String repositoryName
    ) {
    }
}
