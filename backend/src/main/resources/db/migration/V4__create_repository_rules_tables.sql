CREATE TABLE classification_rules (
                                      id BIGSERIAL PRIMARY KEY,
                                      connected_repository_id BIGINT NOT NULL,
                                      path_pattern VARCHAR(500) NOT NULL,
                                      category VARCHAR(50) NOT NULL,
                                      scope VARCHAR(50),
                                      priority INTEGER NOT NULL DEFAULT 100,
                                      enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                      is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_classification_rules_connected_repository
                                          FOREIGN KEY (connected_repository_id)
                                              REFERENCES connected_repositories(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT uk_classification_rules_repository_path
                                          UNIQUE (connected_repository_id, path_pattern),

                                      CONSTRAINT ck_classification_rules_priority
                                          CHECK (priority >= 0)
);

CREATE INDEX idx_classification_rules_repository_enabled_priority
    ON classification_rules (
                             connected_repository_id,
                             enabled,
                             priority
        );


CREATE TABLE convention_rules (
                                  id BIGSERIAL PRIMARY KEY,
                                  connected_repository_id BIGINT NOT NULL,
                                  message_pattern VARCHAR(500) NOT NULL,
                                  commit_type VARCHAR(50),
                                  scope VARCHAR(50),
                                  category VARCHAR(50),
                                  priority INTEGER NOT NULL DEFAULT 100,
                                  enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                  is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_convention_rules_connected_repository
                                      FOREIGN KEY (connected_repository_id)
                                          REFERENCES connected_repositories(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT uk_convention_rules_repository_message
                                      UNIQUE (connected_repository_id, message_pattern),

                                  CONSTRAINT ck_convention_rules_priority
                                      CHECK (priority >= 0),

                                  CONSTRAINT ck_convention_rules_result
                                      CHECK (
                                          commit_type IS NOT NULL
                                              OR scope IS NOT NULL
                                              OR category IS NOT NULL
                                          )
);

CREATE INDEX idx_convention_rules_repository_enabled_priority
    ON convention_rules (
                         connected_repository_id,
                         enabled,
                         priority
        );