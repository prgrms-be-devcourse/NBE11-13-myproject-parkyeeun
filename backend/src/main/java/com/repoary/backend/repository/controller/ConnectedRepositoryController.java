package com.repoary.backend.repository.controller;

import com.repoary.backend.repository.dto.ConnectRepositoryRequest;
import com.repoary.backend.repository.dto.ConnectedRepositoryResponse;
import com.repoary.backend.repository.service.ConnectedRepositoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
public class ConnectedRepositoryController {

    private final ConnectedRepositoryService connectedRepositoryService;

    public ConnectedRepositoryController(ConnectedRepositoryService connectedRepositoryService) {
        this.connectedRepositoryService = connectedRepositoryService;
    }

    @PostMapping("/connect")
    public ConnectedRepositoryResponse connect(
            Authentication authentication,
            @RequestBody ConnectRepositoryRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return connectedRepositoryService.connect(userId, request);
    }

    @GetMapping("/connected")
    public List<ConnectedRepositoryResponse> getConnectedRepositories(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        return connectedRepositoryService.getConnectedRepositories(userId);
    }
}