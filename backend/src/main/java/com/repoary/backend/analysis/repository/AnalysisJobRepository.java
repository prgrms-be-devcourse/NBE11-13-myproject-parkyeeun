package com.repoary.backend.analysis.repository;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.repository.domain.ConnectedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    Optional<AnalysisJob> findByIdAndConnectedRepository(
            Long id,
            ConnectedRepository connectedRepository
    );

    List<AnalysisJob> findAllByConnectedRepositoryOrderByCreatedAtDesc(
            ConnectedRepository connectedRepository
    );

    List<AnalysisJob> findAllByConnectedRepositoryAndTargetDateOrderByCreatedAtDesc(
            ConnectedRepository connectedRepository,
            java.time.LocalDate targetDate
    );
}