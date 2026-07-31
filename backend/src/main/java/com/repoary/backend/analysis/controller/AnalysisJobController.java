package com.repoary.backend.analysis.controller;

import com.repoary.backend.analysis.dto.AnalysisJobSummaryResponse;
import tools.jackson.databind.json.JsonMapper;
import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.dto.AnalysisJobResponse;
import com.repoary.backend.analysis.service.AnalysisJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(
        name = "Analysis Job",
        description = "GitHub 커밋 분석 작업 실행 및 상태 관리 API"
)
@RestController
@RequestMapping(
        "/api/repositories/{connectedRepositoryId}/analysis-jobs"
)
public class AnalysisJobController {

    private final AnalysisJobService analysisJobService;
    private final JsonMapper jsonMapper;

    public AnalysisJobController(
            AnalysisJobService analysisJobService,
            JsonMapper jsonMapper
    ) {
        this.analysisJobService = analysisJobService;
        this.jsonMapper = jsonMapper;
    }

    @Operation(
            summary = "날짜별 커밋 분석 작업 실행",
            description = """
                    지정한 날짜의 GitHub 커밋과 변경 파일을 분석한다.
                    커밋 컨벤션과 파일 분류 규칙을 적용한 뒤
                    TIL 생성에 필요한 축약 결과를 JSONB로 저장한다.

                    분석 날짜는 한국 시간 오전 6시부터
                    다음 날 오전 6시 전까지를 기준으로 한다.
                    """
    )
    @PostMapping
    public AnalysisJobResponse execute(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "분석할 학습 날짜",
                    example = "2026-07-01",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        AnalysisJob analysisJob = analysisJobService.execute(
                userId,
                connectedRepositoryId,
                date
        );

        return AnalysisJobResponse.from(
                analysisJob,
                jsonMapper
        );
    }

    @Operation(
            summary = "분석 작업 상세 조회",
            description = "분석 작업 ID로 상태와 저장된 분석 결과를 조회한다."
    )
    @GetMapping("/{analysisJobId}")
    public AnalysisJobResponse getJob(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "11",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "분석 작업 ID",
                    example = "3",
                    required = true
            )
            @PathVariable Long analysisJobId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        AnalysisJob analysisJob = analysisJobService.getJob(
                userId,
                connectedRepositoryId,
                analysisJobId
        );

        return AnalysisJobResponse.from(
                analysisJob,
                jsonMapper
        );
    }

    @Operation(
            summary = "분석 작업 목록 조회",
            description = """
                연결 저장소의 분석 작업 이력을 최신순으로 조회한다.
                date를 입력하면 해당 학습 날짜의 작업만 조회하고,
                생략하면 저장소의 전체 분석 작업을 조회한다.
                """
    )
    @GetMapping
    public List<AnalysisJobSummaryResponse> getJobs(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "11",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "필터링할 학습 날짜",
                    example = "2026-07-29",
                    required = false
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return analysisJobService
                .getJobs(
                        userId,
                        connectedRepositoryId,
                        date
                )
                .stream()
                .map(AnalysisJobSummaryResponse::from)
                .toList();
    }
}