package com.repoary.backend.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(name = "github_login", nullable = false, length = 100)
    private String githubLogin;

    @Column(name = "github_access_token", columnDefinition = "TEXT")
    private String githubAccessToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(Long githubId, String githubLogin) {
        this.githubId = githubId;
        this.githubLogin = githubLogin;
        this.createdAt = LocalDateTime.now();
    }

    public void updateGitHubAccessToken(String githubAccessToken) {
        this.githubAccessToken = githubAccessToken;
    }

    public Long getId() {
        return id;
    }

    public Long getGithubId() {
        return githubId;
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public String getGithubAccessToken() {
        return githubAccessToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}