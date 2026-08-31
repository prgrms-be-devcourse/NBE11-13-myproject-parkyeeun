package com.repoary.backend.analysis.domain;

import com.repoary.backend.repository.domain.ConnectedRepository;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "analysis_jobs",
        indexes = {
                @Index(
                        name = "idx_analysis_jobs_repository_date",
                        columnList = "connected_repository_id, target_date"
                ),
                @Index(
                        name = "idx_analysis_jobs_repository_created",
                        columnList = "connected_repository_id, created_at"
                )
        }
)
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connected_repository_id", nullable = false)
    private ConnectedRepository connectedRepository;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisJobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String result;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AnalysisJob() {
    }

    public AnalysisJob(
            ConnectedRepository connectedRepository,
            LocalDate targetDate
    ) {
        this.connectedRepository = connectedRepository;
        this.targetDate = targetDate;
        this.status = AnalysisJobStatus.PENDING;
    }

    public void start() {
        if (status != AnalysisJobStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태의 분석 작업만 시작할 수 있습니다."
            );
        }

        this.status = AnalysisJobStatus.RUNNING;
        this.startedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.errorMessage = null;
    }

    public void complete(String result) {
        if (status != AnalysisJobStatus.RUNNING) {
            throw new IllegalStateException(
                    "RUNNING 상태의 분석 작업만 완료할 수 있습니다."
            );
        }

        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException(
                    "분석 결과는 필수입니다."
            );
        }

        this.status = AnalysisJobStatus.COMPLETED;
        this.result = result;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void fail(String errorMessage) {
        if (status != AnalysisJobStatus.RUNNING) {
            throw new IllegalStateException(
                    "RUNNING 상태의 분석 작업만 실패 처리할 수 있습니다."
            );
        }

        this.status = AnalysisJobStatus.FAILED;
        this.result = null;
        this.errorMessage = normalizeErrorMessage(errorMessage);
        this.completedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "분석 중 알 수 없는 오류가 발생했습니다.";
        }

        String trimmedMessage = errorMessage.trim();

        if (trimmedMessage.length() <= 1000) {
            return trimmedMessage;
        }

        return trimmedMessage.substring(0, 1000);
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = AnalysisJobStatus.PENDING;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public ConnectedRepository getConnectedRepository() {
        return connectedRepository;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public AnalysisJobStatus getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}