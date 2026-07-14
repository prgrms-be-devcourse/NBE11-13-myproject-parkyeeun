CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       github_id BIGINT NOT NULL UNIQUE,
                       github_login VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);