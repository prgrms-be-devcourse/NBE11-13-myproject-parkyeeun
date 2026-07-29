package com.repoary.backend.rule.domain;

import com.repoary.backend.repository.domain.ConnectedRepository;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "classification_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_classification_rules_repository_path",
                        columnNames = {"connected_repository_id", "path_pattern"}
                )
        }
)
public class ClassificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connected_repository_id", nullable = false)
    private ConnectedRepository connectedRepository;

    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 50)
    private String scope;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "is_default", nullable = false)
    private boolean defaultRule;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ClassificationRule() {
    }

    public ClassificationRule(
            ConnectedRepository connectedRepository,
            String pathPattern,
            String category,
            String scope,
            int priority,
            boolean defaultRule
    ) {
        this.connectedRepository = connectedRepository;
        this.pathPattern = pathPattern;
        this.category = category;
        this.scope = scope;
        this.priority = priority;
        this.enabled = true;
        this.defaultRule = defaultRule;
    }

    public void update(
            String pathPattern,
            String category,
            String scope,
            int priority
    ) {
        this.pathPattern = pathPattern;
        this.category = category;
        this.scope = scope;
        this.priority = priority;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ConnectedRepository getConnectedRepository() {
        return connectedRepository;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public String getCategory() {
        return category;
    }

    public String getScope() {
        return scope;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDefaultRule() {
        return defaultRule;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}