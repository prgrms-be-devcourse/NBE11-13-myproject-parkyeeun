package com.repoary.backend.rule.controller;

import com.repoary.backend.rule.dto.ClassificationRuleRequest;
import com.repoary.backend.rule.dto.ClassificationRuleResponse;
import com.repoary.backend.rule.dto.ConventionRuleRequest;
import com.repoary.backend.rule.dto.ConventionRuleResponse;
import com.repoary.backend.rule.service.RepositoryRuleCommandService;
import com.repoary.backend.rule.service.RepositoryRuleQueryService;
import com.repoary.backend.rule.service.RepositoryRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/classifications")
    public List<ClassificationRuleResponse> getClassificationRules(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleQueryService.getClassificationRules(
                userId,
                connectedRepositoryId
        );
    }

    @PostMapping("/classifications")
    public ClassificationRuleResponse createClassificationRule(
            Authentication authentication,
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

    @PutMapping("/classifications/{ruleId}")
    public ClassificationRuleResponse updateClassificationRule(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId,
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

    @PatchMapping("/classifications/{ruleId}/enabled")
    public ClassificationRuleResponse updateClassificationEnabled(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId,
            @PathVariable Long ruleId,
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

    @DeleteMapping("/classifications/{ruleId}")
    public ResponseEntity<Void> deleteClassificationRule(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId,
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

    @GetMapping("/conventions")
    public List<ConventionRuleResponse> getConventionRules(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return repositoryRuleQueryService.getConventionRules(
                userId,
                connectedRepositoryId
        );
    }

    @PostMapping("/conventions")
    public ConventionRuleResponse createConventionRule(
            Authentication authentication,
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

    @PutMapping("/conventions/{ruleId}")
    public ConventionRuleResponse updateConventionRule(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId,
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

    @PatchMapping("/conventions/{ruleId}/enabled")
    public ConventionRuleResponse updateConventionEnabled(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId,
            @PathVariable Long ruleId,
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

    @DeleteMapping("/conventions/{ruleId}")
    public ResponseEntity<Void> deleteConventionRule(
            Authentication authentication,
            @PathVariable Long connectedRepositoryId,
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

    @PostMapping("/defaults/restore")
    public ResponseEntity<Void> restoreDefaultRules(
            Authentication authentication,
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