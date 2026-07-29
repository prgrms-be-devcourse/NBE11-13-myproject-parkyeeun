package com.repoary.backend.rule.repository;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassificationRuleRepository extends JpaRepository<ClassificationRule, Long> {

    List<ClassificationRule> findAllByConnectedRepositoryOrderByPriorityAsc(
            ConnectedRepository connectedRepository
    );

    List<ClassificationRule> findAllByConnectedRepositoryAndEnabledTrueOrderByPriorityAsc(
            ConnectedRepository connectedRepository
    );

    Optional<ClassificationRule> findByIdAndConnectedRepository(
            Long id,
            ConnectedRepository connectedRepository
    );

    boolean existsByConnectedRepositoryAndPathPattern(
            ConnectedRepository connectedRepository,
            String pathPattern
    );

    boolean existsByConnectedRepositoryAndPathPatternAndIdNot(
            ConnectedRepository connectedRepository,
            String pathPattern,
            Long id
    );
}