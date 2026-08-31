package com.repoary.backend.til.service;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.analysis.repository.AnalysisJobRepository;
import com.repoary.backend.common.exception.ConflictException;
import com.repoary.backend.common.exception.NotFoundException;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.til.domain.TilDocument;
import com.repoary.backend.til.repository.TilDocumentRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

@Service
public class TilService {

    private final TilDocumentRepository tilDocumentRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final TilMarkdownGenerator tilMarkdownGenerator;
    private final JsonMapper jsonMapper;

    public TilService(
            TilDocumentRepository tilDocumentRepository,
            AnalysisJobRepository analysisJobRepository,
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            TilMarkdownGenerator tilMarkdownGenerator,
            JsonMapper jsonMapper
    ) {
        this.tilDocumentRepository = tilDocumentRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.tilMarkdownGenerator = tilMarkdownGenerator;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public TilDocument createDraft(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        if (targetDate == null) {
            throw new IllegalArgumentException(
                    "TIL 날짜는 필수입니다."
            );
        }

        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        if (tilDocumentRepository
                .existsByConnectedRepositoryAndTargetDate(
                        connectedRepository,
                        targetDate
                )) {
            throw new ConflictException(
                    "해당 날짜의 TIL이 이미 존재합니다."
            );
        }

        AnalysisJob analysisJob =
                analysisJobRepository
                        .findFirstByConnectedRepositoryAndTargetDateAndStatusOrderByCreatedAtDesc(
                                connectedRepository,
                                targetDate,
                                AnalysisJobStatus.COMPLETED
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "완료된 분석 결과를 찾을 수 없습니다."
                                )
                        );

        StoredAnalysisResult analysisResult =
                parseAnalysisResult(
                        analysisJob.getResult()
                );

        String content =
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                );

        String title =
                targetDate + " TIL (Today I Learned)";

        TilDocument tilDocument =
                new TilDocument(
                        connectedRepository,
                        analysisJob,
                        targetDate,
                        title,
                        content
                );

        return tilDocumentRepository.save(tilDocument);
    }

    @Transactional(readOnly = true)
    public TilDocument getByDate(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        if (targetDate == null) {
            throw new IllegalArgumentException(
                    "TIL 날짜는 필수입니다."
            );
        }

        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        return tilDocumentRepository
                .findByConnectedRepositoryAndTargetDate(
                        connectedRepository,
                        targetDate
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "TIL을 찾을 수 없습니다."
                        )
                );
    }

    @Transactional(readOnly = true)
    public TilDocument getDocument(
            Long userId,
            Long connectedRepositoryId,
            Long tilDocumentId
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        return tilDocumentRepository
                .findByIdAndConnectedRepository(
                        tilDocumentId,
                        connectedRepository
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "TIL을 찾을 수 없습니다."
                        )
                );
    }

    @Transactional
    public TilDocument updateContent(
            Long userId,
            Long connectedRepositoryId,
            Long tilDocumentId,
            String content
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        TilDocument tilDocument =
                tilDocumentRepository
                        .findByIdAndConnectedRepository(
                                tilDocumentId,
                                connectedRepository
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "TIL을 찾을 수 없습니다."
                                )
                        );

        tilDocument.updateContent(content);

        return tilDocument;
    }

    private StoredAnalysisResult parseAnalysisResult(
            String resultJson
    ) {
        if (resultJson == null || resultJson.isBlank()) {
            throw new IllegalStateException(
                    "저장된 분석 결과가 없습니다."
            );
        }

        try {
            return jsonMapper.readValue(
                    resultJson,
                    StoredAnalysisResult.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "저장된 분석 결과를 읽을 수 없습니다.",
                    exception
            );
        }
    }

    private ConnectedRepository getOwnedConnectedRepository(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return connectedRepositoryRepository
                .findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "연결된 저장소를 찾을 수 없습니다."
                        )
                );
    }
}