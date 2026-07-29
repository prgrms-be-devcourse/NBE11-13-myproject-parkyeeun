package com.repoary.backend.rule.domain;

import com.repoary.backend.repository.domain.ConnectedRepository;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "convention_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_convention_rules_repository_message",
                        columnNames = {"connected_repository_id", "message_pattern"}
                )
        }
)
public class ConventionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connected_repository_id", nullable = false)
    private ConnectedRepository connectedRepository;

    @Column(name = "message_pattern", nullable = false, length = 500)
    private String messagePattern;

    @Column(name = "commit_type", length = 50)
    private String commitType;

    @Column(length = 50)
    private String scope;

    @Column(length = 50)
    private String category;

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

    protected ConventionRule() {
    }

    public ConventionRule(
            ConnectedRepository connectedRepository,
            String messagePattern,
            String commitType,
            String scope,
            String category,
            int priority,
            boolean defaultRule
    ) {
        this.connectedRepository = connectedRepository;
        this.messagePattern = messagePattern;
        this.commitType = commitType;
        this.scope = scope;
        this.category = category;
        this.priority = priority;
        this.enabled = true;
        this.defaultRule = defaultRule;
    }

    public void update(
            String messagePattern,
            String commitType,
            String scope,
            String category,
            int priority
    ) {
        this.messagePattern = messagePattern;
        this.commitType = commitType;
        this.scope = scope;
        this.category = category;
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

    public String getMessagePattern() {
        return messagePattern;
    }

    public String getCommitType() {
        return commitType;
    }

    public String getScope() {
        return scope;
    }

    public String getCategory() {
        return category;
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