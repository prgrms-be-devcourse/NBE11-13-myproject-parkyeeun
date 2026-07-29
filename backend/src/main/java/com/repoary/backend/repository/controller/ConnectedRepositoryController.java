package com.repoary.backend.repository.controller;

import com.repoary.backend.repository.dto.ConnectRepositoryRequest;
import com.repoary.backend.repository.dto.ConnectedRepositoryResponse;
import com.repoary.backend.repository.service.ConnectedRepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Connected Repository",
        description = "GitHub 저장소 연결 및 연결 해제 API"
)
@RestController
@RequestMapping("/api/repositories")
public class ConnectedRepositoryController {

    private final ConnectedRepositoryService connectedRepositoryService;

    public ConnectedRepositoryController(ConnectedRepositoryService connectedRepositoryService) {
        this.connectedRepositoryService = connectedRepositoryService;
    }

    @Operation(
            summary = "GitHub 저장소 연결",
            description = "로그인한 사용자의 Repoary 계정에 GitHub 저장소를 연결하고 기본 규칙을 생성한다."
    )
    @PostMapping("/connect")
    public ConnectedRepositoryResponse connect(
            Authentication authentication,
            @RequestBody ConnectRepositoryRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return connectedRepositoryService.connect(userId, request);
    }

    @Operation(
            summary = "연결된 저장소 목록 조회",
            description = "로그인한 사용자가 Repoary에 연결한 GitHub 저장소 목록을 조회한다."
    )
    @GetMapping("/connected")
    public List<ConnectedRepositoryResponse> getConnectedRepositories(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return connectedRepositoryService.getConnectedRepositories(userId);
    }

    @Operation(
            summary = "GitHub 저장소 연결 해제",
            description = "로그인한 사용자의 Repoary 계정에서 GitHub 저장소 연결을 해제한다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "저장소 연결 해제 성공"
    )
    @DeleteMapping("/connected/{githubRepositoryId}")
    public ResponseEntity<Void> disconnect(
            Authentication authentication,
            @Parameter(
                    description = "GitHub 저장소 고유 ID",
                    example = "123456789"
            )
            @PathVariable Long githubRepositoryId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        connectedRepositoryService.disconnect(userId, githubRepositoryId);

        return ResponseEntity.noContent().build();
    }
}