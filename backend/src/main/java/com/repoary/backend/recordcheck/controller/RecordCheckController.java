package com.repoary.backend.recordcheck.controller;

import com.repoary.backend.recordcheck.dto.RecordCheckResponse;
import com.repoary.backend.recordcheck.service.RecordCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Tag(
        name = "Record Check",
        description = "TIL / README 누락 점검 API"
)
@RestController
@RequestMapping(
        "/api/repositories/{connectedRepositoryId}/record-checks"
)
public class RecordCheckController {

    private final RecordCheckService recordCheckService;

    public RecordCheckController(
            RecordCheckService recordCheckService
    ) {
        this.recordCheckService = recordCheckService;
    }

    @Operation(
            summary = "월별 TIL / README 누락 점검",
            description = """
                    완료된 커밋 분석 결과가 존재하는 날짜를 기준으로
                    실제 GitHub의 TIL 파일과 월별 README 반영 여부를 점검한다.

                    GitHub 파일은 조회만 수행하며 수정하지 않는다.
                    """
    )
    @GetMapping
    public RecordCheckResponse check(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "점검할 월",
                    example = "2026-09",
                    required = true
            )
            @RequestParam YearMonth month
    ) {
        Long userId =
                (Long) authentication.getPrincipal();

        return recordCheckService.check(
                userId,
                connectedRepositoryId,
                month
        );
    }
}