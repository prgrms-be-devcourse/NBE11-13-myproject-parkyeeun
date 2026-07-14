CREATE TABLE connected_repositories (
                                        id BIGSERIAL PRIMARY KEY,
                                        user_id BIGINT NOT NULL,
                                        github_repository_id BIGINT NOT NULL,
                                        name VARCHAR(100) NOT NULL,
                                        full_name VARCHAR(200) NOT NULL,
                                        html_url VARCHAR(500) NOT NULL,
                                        private_repository BOOLEAN NOT NULL,
                                        default_branch VARCHAR(100) NOT NULL,
                                        connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_connected_repositories_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES users(id)
                                                ON DELETE CASCADE,

                                        CONSTRAINT uk_connected_repositories_user_repository
                                            UNIQUE (user_id, github_repository_id)
);