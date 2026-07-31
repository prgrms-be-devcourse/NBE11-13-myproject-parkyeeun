package com.repoary.backend.github.controller;

import com.repoary.backend.github.dto.GitHubCommitDetailResponse;
import com.repoary.backend.github.dto.GitHubCommitSummaryResponse;
import com.repoary.backend.github.service.GitHubCommitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(
        name = "GitHub Commit",
        description = "연결된 GitHub 저장소의 커밋 조회 API"
)
@RestController
@RequestMapping("/api/repositories/{connectedRepositoryId}/commits")
public class GitHubCommitController {

    private final GitHubCommitService gitHubCommitService;

    public GitHubCommitController(
            GitHubCommitService gitHubCommitService
    ) {
        this.gitHubCommitService = gitHubCommitService;
    }

    @Operation(
            summary = "날짜별 커밋 목록 조회",
            description = """
                    연결된 저장소의 기본 브랜치에서 선택한 학습일의 커밋 목록을 조회한다.
                    학습일은 오전 6시부터 다음 날 오전 6시 직전까지를 기준으로 한다.
                    """
    )
    @GetMapping
    public List<GitHubCommitSummaryResponse> getCommits(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1"
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "조회할 학습 날짜",
                    example = "2026-07-30"
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return gitHubCommitService
                .getCommits(userId, connectedRepositoryId, date)
                .stream()
                .map(GitHubCommitSummaryResponse::from)
                .toList();
    }

    @Operation(
            summary = "커밋 변경 파일 상세 조회",
            description = "지정한 커밋 SHA의 변경 파일과 추가·삭제 줄 수를 조회한다."
    )
    @GetMapping("/{commitSha}")
    public GitHubCommitDetailResponse getCommitDetail(
            Authentication authentication,
            @Parameter(description = "Repoary 내부 연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "조회할 커밋 SHA")
            @PathVariable String commitSha
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return gitHubCommitService.getCommitDetail(
                userId,
                connectedRepositoryId,
                commitSha
        );
    }
}