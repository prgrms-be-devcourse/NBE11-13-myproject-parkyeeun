package com.repoary.backend.repository.repository;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectedRepositoryRepository extends JpaRepository<ConnectedRepository, Long> {

    Optional<ConnectedRepository> findByUserAndGithubRepositoryId(User user, Long githubRepositoryId);

    List<ConnectedRepository> findAllByUserOrderByConnectedAtDesc(User user);
}