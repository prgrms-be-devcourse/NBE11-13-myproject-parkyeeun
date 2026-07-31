CREATE TABLE analysis_jobs
(
    id                      BIGSERIAL PRIMARY KEY,
    connected_repository_id BIGINT       NOT NULL,
    target_date             DATE         NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    result                  JSONB,
    error_message           VARCHAR(1000),
    started_at              TIMESTAMP,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,

    CONSTRAINT fk_analysis_jobs_repository
        FOREIGN KEY (connected_repository_id)
            REFERENCES connected_repositories (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_analysis_jobs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_analysis_jobs_repository_date
    ON analysis_jobs (connected_repository_id, target_date);

CREATE INDEX idx_analysis_jobs_repository_created
    ON analysis_jobs (connected_repository_id, created_at DESC);