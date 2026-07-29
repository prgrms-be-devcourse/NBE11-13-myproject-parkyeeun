package com.repoary.backend.rule.controller;

import com.repoary.backend.rule.dto.ClassificationRuleRequest;
import com.repoary.backend.rule.dto.ClassificationRuleResponse;
import com.repoary.backend.rule.dto.ConventionRuleRequest;
import com.repoary.backend.rule.dto.ConventionRuleResponse;
import com.repoary.backend.rule.service.RepositoryRuleCommandService;
import com.repoary.backend.rule.service.RepositoryRuleQueryService;
import com.repoary.backend.rule.service.RepositoryRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Repository Rule",
        description = "연결된 GitHub 저장소의 경로 분류 규칙과 커밋 규칙 관리 API"
)
@RestController
@RequestMapping("/api/repositories/{connectedRepositoryId}/rules")
public class RepositoryRuleController {

    private final RepositoryRuleQueryService repositoryRuleQueryService;
    private final RepositoryRuleCommandService repositoryRuleCommandService;
    private final RepositoryRuleService repositoryRuleService;

    public RepositoryRuleController(
            RepositoryRuleQueryService repositoryRuleQueryService,
            RepositoryRuleCommandService repositoryRuleCommandService,
            RepositoryRuleService repositoryRuleService
    ) {
        this.repositoryRuleQueryService = repositoryRuleQueryService;
        this.repositoryRuleCommandService = repositoryRuleCommandService;
        this.repositoryRuleService = repositoryRuleService;
    }

    @Operation(
            summary = "경로 분류 규칙 목록 조회",
            description = "연결된 저장소에 등록된 경로 분류 규칙을 조회한다."
    )
    @GetMapping("/classifications")
    public List<ClassificationRuleResponse> getClassificationRules(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleQueryService.getClassificationRules(
                userId,
                connectedRepositoryId
        );
    }

    @Operation(
            summary = "경로 분류 규칙 추가",
            description = "연결된 저장소에 새로운 경로 분류 규칙을 추가한다."
    )
    @PostMapping("/classifications")
    public ClassificationRuleResponse createClassificationRule(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @RequestBody ClassificationRuleRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleCommandService.createClassificationRule(
                userId,
                connectedRepositoryId,
                request
        );
    }

    @Operation(
            summary = "경로 분류 규칙 수정",
            description = "등록된 경로 분류 규칙의 정보를 수정한다."
    )
    @PutMapping("/classifications/{ruleId}")
    public ClassificationRuleResponse updateClassificationRule(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "경로 분류 규칙 ID", example = "1")
            @PathVariable Long ruleId,
            @RequestBody ClassificationRuleRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleCommandService.updateClassificationRule(
                userId,
                connectedRepositoryId,
                ruleId,
                request
        );
    }

    @Operation(
            summary = "경로 분류 규칙 활성화 상태 변경",
            description = "경로 분류 규칙의 활성화 여부를 변경한다."
    )
    @PatchMapping("/classifications/{ruleId}/enabled")
    public ClassificationRuleResponse updateClassificationEnabled(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "경로 분류 규칙 ID", example = "1")
            @PathVariable Long ruleId,
            @Parameter(description = "변경할 활성화 상태", example = "false")
            @RequestParam boolean enabled
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleCommandService.updateClassificationEnabled(
                userId,
                connectedRepositoryId,
                ruleId,
                enabled
        );
    }

    @Operation(
            summary = "경로 분류 규칙 삭제",
            description = "연결된 저장소에서 경로 분류 규칙을 삭제한다."
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping("/classifications/{ruleId}")
    public ResponseEntity<Void> deleteClassificationRule(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "경로 분류 규칙 ID", example = "1")
            @PathVariable Long ruleId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        repositoryRuleCommandService.deleteClassificationRule(
                userId,
                connectedRepositoryId,
                ruleId
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "커밋 규칙 목록 조회",
            description = "연결된 저장소에 등록된 커밋 규칙을 조회한다."
    )
    @GetMapping("/conventions")
    public List<ConventionRuleResponse> getConventionRules(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleQueryService.getConventionRules(
                userId,
                connectedRepositoryId
        );
    }

    @Operation(
            summary = "커밋 규칙 추가",
            description = "연결된 저장소에 새로운 커밋 규칙을 추가한다."
    )
    @PostMapping("/conventions")
    public ConventionRuleResponse createConventionRule(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @RequestBody ConventionRuleRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleCommandService.createConventionRule(
                userId,
                connectedRepositoryId,
                request
        );
    }

    @Operation(
            summary = "커밋 규칙 수정",
            description = "등록된 커밋 규칙의 정보를 수정한다."
    )
    @PutMapping("/conventions/{ruleId}")
    public ConventionRuleResponse updateConventionRule(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "커밋 규칙 ID", example = "1")
            @PathVariable Long ruleId,
            @RequestBody ConventionRuleRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleCommandService.updateConventionRule(
                userId,
                connectedRepositoryId,
                ruleId,
                request
        );
    }

    @Operation(
            summary = "커밋 규칙 활성화 상태 변경",
            description = "커밋 규칙의 활성화 여부를 변경한다."
    )
    @PatchMapping("/conventions/{ruleId}/enabled")
    public ConventionRuleResponse updateConventionEnabled(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "커밋 규칙 ID", example = "1")
            @PathVariable Long ruleId,
            @Parameter(description = "변경할 활성화 상태", example = "false")
            @RequestParam boolean enabled
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleCommandService.updateConventionEnabled(
                userId,
                connectedRepositoryId,
                ruleId,
                enabled
        );
    }

    @Operation(
            summary = "커밋 규칙 삭제",
            description = "연결된 저장소에서 커밋 규칙을 삭제한다."
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping("/conventions/{ruleId}")
    public ResponseEntity<Void> deleteConventionRule(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId,
            @Parameter(description = "커밋 규칙 ID", example = "1")
            @PathVariable Long ruleId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        repositoryRuleCommandService.deleteConventionRule(
                userId,
                connectedRepositoryId,
                ruleId
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "기본 규칙 복원",
            description = "삭제된 기본 경로 분류 규칙과 기본 커밋 규칙을 다시 생성한다."
    )
    @ApiResponse(responseCode = "204", description = "기본 규칙 복원 성공")
    @PostMapping("/defaults/restore")
    public ResponseEntity<Void> restoreDefaultRules(
            Authentication authentication,
            @Parameter(description = "연결 저장소 ID", example = "1")
            @PathVariable Long connectedRepositoryId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        repositoryRuleService.restoreDefaultRules(
                userId,
                connectedRepositoryId
        );

        return ResponseEntity.noContent().build();
    }
}