package com.repoary.backend.user.controller;

import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.dto.UserResponse;
import com.repoary.backend.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/me")
    public UserResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userService.getUser(userId);

        return UserResponse.from(user);
    }
}