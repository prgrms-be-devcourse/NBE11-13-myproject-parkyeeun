package com.repoary.backend.repository.domain;

import com.repoary.backend.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "connected_repositories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_connected_repositories_user_repository",
                        columnNames = {"user_id", "github_repository_id"}
                )
        }
)
public class ConnectedRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repository_id", nullable = false)
    private Long githubRepositoryId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "private_repository", nullable = false)
    private boolean privateRepository;

    @Column(name = "default_branch", nullable = false, length = 100)
    private String defaultBranch;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    protected ConnectedRepository() {
    }

    public ConnectedRepository(
            User user,
            Long githubRepositoryId,
            String name,
            String fullName,
            String htmlUrl,
            boolean privateRepository,
            String defaultBranch
    ) {
        this.user = user;
        this.githubRepositoryId = githubRepositoryId;
        this.name = name;
        this.fullName = fullName;
        this.htmlUrl = htmlUrl;
        this.privateRepository = privateRepository;
        this.defaultBranch = defaultBranch;
        this.connectedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Long getGithubRepositoryId() {
        return githubRepositoryId;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public boolean isPrivateRepository() {
        return privateRepository;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
}