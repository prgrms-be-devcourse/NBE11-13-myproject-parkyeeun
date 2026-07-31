package com.repoary.backend.analysis.controller;

import com.repoary.backend.analysis.dto.CommitConsistencyRequest;
import com.repoary.backend.analysis.dto.CommitConsistencyResponse;
import com.repoary.backend.analysis.service.CommitConsistencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Commit Consistency",
        description = "기간별 커밋 메시지의 컨벤션 일관성을 분석하는 API"
)
@RestController
@RequestMapping(
        "/api/repositories/{connectedRepositoryId}/consistency-analysis"
)
public class CommitConsistencyController {

    private final CommitConsistencyService commitConsistencyService;

    public CommitConsistencyController(
            CommitConsistencyService commitConsistencyService
    ) {
        this.commitConsistencyService =
                commitConsistencyService;
    }

    @Operation(
            summary = "커밋 컨벤션 일관성 분석",
            description = """
                    지정한 기간의 GitHub 커밋을 조회하고,
                    변경 파일의 경로 규칙을 기준으로 작업 영역을 분류한다.

                    같은 작업 영역에 속한 커밋을 비교하여
                    type, scope, 커밋 메시지 패턴의 일관성을 분석한다.

                    조회 날짜는 한국 시간 오전 6시부터
                    다음 날 오전 6시 전까지를 기준으로 한다.
                    """
    )
    @PostMapping
    public CommitConsistencyResponse analyzeConsistency(
            Authentication authentication,
            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,
            @RequestBody CommitConsistencyRequest request
    ) {
        Long userId =
                (Long) authentication.getPrincipal();

        return commitConsistencyService.analyze(
                userId,
                connectedRepositoryId,
                request.from(),
                request.to()
        );
    }
}