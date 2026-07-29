package com.repoary.backend.user.controller;

import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.dto.UserResponse;
import com.repoary.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "User",
        description = "로그인 사용자 정보 조회 API"
)
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "내 정보 조회",
            description = "JWT로 인증된 현재 사용자의 Repoary 계정 정보를 조회한다."
    )
    @GetMapping("/api/users/me")
    public UserResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.getUser(userId);

        return UserResponse.from(user);
    }
}