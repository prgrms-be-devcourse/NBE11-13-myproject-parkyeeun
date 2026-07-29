package com.repoary.backend.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(
        name = "Health",
        description = "백엔드 서버 상태 확인 API"
)
@RestController
public class HealthController {

    @Operation(
            summary = "서버 상태 확인",
            description = "Repoary 백엔드 서버가 정상적으로 실행 중인지 확인한다."
    )
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "OK",
                "service", "repoary-backend",
                "time", LocalDateTime.now()
        );
    }
}