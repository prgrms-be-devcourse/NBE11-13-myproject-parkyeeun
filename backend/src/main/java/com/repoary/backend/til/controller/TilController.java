package com.repoary.backend.til.controller;

import com.repoary.backend.til.domain.TilDocument;
import com.repoary.backend.til.dto.TilDocumentResponse;
import com.repoary.backend.til.dto.TilUpdateRequest;
import com.repoary.backend.til.service.TilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(
        name = "TIL",
        description = "TIL Markdown 초안 생성 및 관리 API"
)
@RestController
@RequestMapping(
        "/api/repositories/{connectedRepositoryId}/til-documents"
)
public class TilController {

    private final TilService tilService;

    public TilController(
            TilService tilService
    ) {
        this.tilService = tilService;
    }

    @Operation(
            summary = "TIL 초안 생성",
            description = """
                    완료된 날짜별 커밋 분석 결과를 기반으로
                    TIL Markdown 초안을 생성한다.

                    GitHub 커밋을 다시 조회하지 않고
                    저장된 analysis_jobs 결과를 사용한다.
                    """
    )
    @PostMapping
    public TilDocumentResponse createDraft(
            Authentication authentication,

            @Parameter(
                    description = "Repoary 내부 연결 저장소 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "TIL을 생성할 학습 날짜",
                    example = "2026-08-28",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        TilDocument tilDocument =
                tilService.createDraft(
                        userId,
                        connectedRepositoryId,
                        date
                );

        return TilDocumentResponse.from(tilDocument);
    }

    @Operation(
            summary = "날짜별 TIL 조회",
            description = "연결 저장소의 특정 날짜 TIL 문서를 조회한다."
    )
    @GetMapping
    public TilDocumentResponse getByDate(
            Authentication authentication,

            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "조회할 TIL 날짜",
                    example = "2026-08-28",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return TilDocumentResponse.from(
                tilService.getByDate(
                        userId,
                        connectedRepositoryId,
                        date
                )
        );
    }

    @Operation(
            summary = "TIL 상세 조회",
            description = "TIL 문서 ID로 Markdown 내용을 조회한다."
    )
    @GetMapping("/{tilDocumentId}")
    public TilDocumentResponse getDocument(
            Authentication authentication,

            @PathVariable Long connectedRepositoryId,

            @Parameter(
                    description = "TIL 문서 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long tilDocumentId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return TilDocumentResponse.from(
                tilService.getDocument(
                        userId,
                        connectedRepositoryId,
                        tilDocumentId
                )
        );
    }

    @Operation(
            summary = "TIL Markdown 수정",
            description = "사용자가 편집한 TIL Markdown 내용을 저장한다."
    )
    @PatchMapping("/{tilDocumentId}")
    public TilDocumentResponse updateContent(
            Authentication authentication,

            @PathVariable Long connectedRepositoryId,

            @PathVariable Long tilDocumentId,

            @RequestBody TilUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        TilDocument tilDocument =
                tilService.updateContent(
                        userId,
                        connectedRepositoryId,
                        tilDocumentId,
                        request.content()
                );

        return TilDocumentResponse.from(tilDocument);
    }
}