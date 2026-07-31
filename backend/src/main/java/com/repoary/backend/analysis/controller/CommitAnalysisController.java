package com.repoary.backend.analysis.controller;

import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
import com.repoary.backend.analysis.service.CommitAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(
        name = "Commit Analysis",
        description = "GitHub 커밋과 변경 파일을 저장소 규칙에 따라 분석하는 API"
)
@RestController
@RequestMapping("/api/repositories/{connectedRepositoryId}/analysis")
public class CommitAnalysisController {

    private final CommitAnalysisService commitAnalysisService;

    public CommitAnalysisController(
            CommitAnalysisService commitAnalysisService
    ) {
        this.commitAnalysisService = commitAnalysisService;
    }

    @Operation(
            summary = "날짜별 커밋 분석",
            description = """
                    지정한 날짜의 GitHub 커밋을 조회하고,
                    커밋 메시지에는 커밋 컨벤션 규칙을 적용하며,
                    변경 파일 경로에는 분류 규칙을 적용한다.
                    
                    분석 날짜는 한국 시간 오전 6시부터 다음 날 오전 6시 전까지를 기준으로 한다.
                    """
    )
    @GetMapping("/commits")
    public List<CommitAnalysisResponse> analyzeCommits(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "분석할 학습 날짜",
                    example = "2026-07-29",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return commitAnalysisService.analyzeCommits(
                userId,
                connectedRepositoryId,
                date
        );
    }
}