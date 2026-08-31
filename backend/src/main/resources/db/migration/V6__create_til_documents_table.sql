CREATE TABLE til_documents
(
    id BIGSERIAL PRIMARY KEY,

    connected_repository_id BIGINT NOT NULL,
    analysis_job_id BIGINT NOT NULL,

    target_date DATE NOT NULL,

    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_til_documents_repository
        FOREIGN KEY (connected_repository_id)
            REFERENCES connected_repositories (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_til_documents_analysis_job
        FOREIGN KEY (analysis_job_id)
            REFERENCES analysis_jobs (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_til_documents_repository_date
        UNIQUE (connected_repository_id, target_date),

    CONSTRAINT ck_til_documents_status
        CHECK (status IN ('DRAFT'))
);

CREATE INDEX idx_til_documents_repository_date
    ON til_documents (connected_repository_id, target_date);

CREATE INDEX idx_til_documents_repository_created
    ON til_documents (connected_repository_id, created_at DESC);