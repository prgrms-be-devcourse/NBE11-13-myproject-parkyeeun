package com.repoary.backend.rule.repository;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.rule.domain.ConventionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConventionRuleRepository extends JpaRepository<ConventionRule, Long> {

    List<ConventionRule> findAllByConnectedRepositoryOrderByPriorityAsc(
            ConnectedRepository connectedRepository
    );

    List<ConventionRule> findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
            ConnectedRepository connectedRepository
    );

    Optional<ConventionRule> findByIdAndConnectedRepository(
            Long id,
            ConnectedRepository connectedRepository
    );

    boolean existsByConnectedRepositoryAndMessagePattern(
            ConnectedRepository connectedRepository,
            String messagePattern
    );

    boolean existsByConnectedRepositoryAndMessagePatternAndIdNot(
            ConnectedRepository connectedRepository,
            String messagePattern,
            Long id
    );
}