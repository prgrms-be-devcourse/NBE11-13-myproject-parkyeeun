package com.repoary.backend.readme.controller;

import com.repoary.backend.readme.dto.ReadmeRowResponse;
import com.repoary.backend.readme.service.ReadmeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(
        name = "README",
        description = "월별 README Summary 행 생성 API"
)
@RestController
@RequestMapping(
        "/api/repositories/{connectedRepositoryId}/readme-rows"
)
public class ReadmeController {

    private final ReadmeService readmeService;

    public ReadmeController(
            ReadmeService readmeService
    ) {
        this.readmeService = readmeService;
    }

    @Operation(
            summary = "README 행 생성",
            description = """
                    저장된 TIL을 기반으로
                    월별 README에 추가할 Summary 행을 생성한다.

                    생성 결과는 별도로 저장하지 않는다.
                    """
    )
    @PostMapping
    public ReadmeRowResponse generateRow(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "README 행을 생성할 TIL 날짜",
                    example = "2026-09-10",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return readmeService.generateRow(
                userId,
                connectedRepositoryId,
                date
        );
    }
}