package com.repoary.backend.til.repository;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.til.domain.TilDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TilDocumentRepository extends JpaRepository<TilDocument, Long> {

    boolean existsByConnectedRepositoryAndTargetDate(
            ConnectedRepository connectedRepository,
            LocalDate targetDate
    );

    Optional<TilDocument> findByConnectedRepositoryAndTargetDate(
            ConnectedRepository connectedRepository,
            LocalDate targetDate
    );

    Optional<TilDocument> findByIdAndConnectedRepository(
            Long id,
            ConnectedRepository connectedRepository
    );

    List<TilDocument> findAllByConnectedRepositoryOrderByTargetDateDesc(
            ConnectedRepository connectedRepository
    );
}