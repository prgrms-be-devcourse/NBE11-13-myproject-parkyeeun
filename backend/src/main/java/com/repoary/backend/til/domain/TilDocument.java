package com.repoary.backend.til.domain;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.repository.domain.ConnectedRepository;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "til_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_til_documents_repository_date",
                        columnNames = {"connected_repository_id", "target_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_til_documents_repository_date",
                        columnList = "connected_repository_id, target_date"
                ),
                @Index(
                        name = "idx_til_documents_repository_created",
                        columnList = "connected_repository_id, created_at"
                )
        }
)
public class TilDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connected_repository_id", nullable = false)
    private ConnectedRepository connectedRepository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_job_id", nullable = false)
    private AnalysisJob analysisJob;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TilDocumentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TilDocument() {
    }

    public TilDocument(
            ConnectedRepository connectedRepository,
            AnalysisJob analysisJob,
            LocalDate targetDate,
            String title,
            String content
    ) {
        if (connectedRepository == null) {
            throw new IllegalArgumentException("연결 저장소는 필수입니다.");
        }

        if (analysisJob == null) {
            throw new IllegalArgumentException("분석 작업은 필수입니다.");
        }

        if (targetDate == null) {
            throw new IllegalArgumentException("TIL 날짜는 필수입니다.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("TIL 제목은 필수입니다.");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("TIL 내용은 필수입니다.");
        }

        this.connectedRepository = connectedRepository;
        this.analysisJob = analysisJob;
        this.targetDate = targetDate;
        this.title = title.trim();
        this.content = content;
        this.status = TilDocumentStatus.DRAFT;
    }

    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("TIL 내용은 필수입니다.");
        }

        this.content = content;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = TilDocumentStatus.DRAFT;
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

    public AnalysisJob getAnalysisJob() {
        return analysisJob;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public TilDocumentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}