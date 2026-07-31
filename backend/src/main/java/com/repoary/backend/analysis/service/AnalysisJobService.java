package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import tools.jackson.databind.json.JsonMapper;
import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.analysis.repository.AnalysisJobRepository;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final CommitAnalysisService commitAnalysisService;
    private final StoredAnalysisResultMapper storedAnalysisResultMapper;
    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final JsonMapper jsonMapper;

    public AnalysisJobService(
            AnalysisJobRepository analysisJobRepository,
            CommitAnalysisService commitAnalysisService,
            StoredAnalysisResultMapper storedAnalysisResultMapper,
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            JsonMapper jsonMapper
    ) {
        this.analysisJobRepository = analysisJobRepository;
        this.commitAnalysisService = commitAnalysisService;
        this.storedAnalysisResultMapper = storedAnalysisResultMapper;
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.jsonMapper = jsonMapper;
    }

    public AnalysisJob execute(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        if (targetDate == null) {
            throw new IllegalArgumentException("분석 날짜는 필수입니다.");
        }

        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        AnalysisJob analysisJob = new AnalysisJob(
                connectedRepository,
                targetDate
        );

        analysisJob = analysisJobRepository.saveAndFlush(analysisJob);

        try {
            analysisJob.start();
            analysisJob = analysisJobRepository.saveAndFlush(analysisJob);

            List<CommitAnalysisResponse> analysisResponses =
                    commitAnalysisService.analyzeCommits(
                            userId,
                            connectedRepositoryId,
                            targetDate
                    );

            StoredAnalysisResult storedResult =
                    storedAnalysisResultMapper.map(
                            targetDate,
                            analysisResponses
                    );

            String resultJson = jsonMapper
                    .valueToTree(storedResult)
                    .toString();

            analysisJob.complete(resultJson);

            return analysisJobRepository.saveAndFlush(analysisJob);
        } catch (Exception exception) {
            AnalysisJob failedJob = analysisJobRepository
                    .findById(analysisJob.getId())
                    .orElse(null);

            if (failedJob != null
                    && failedJob.getStatus() == AnalysisJobStatus.RUNNING) {
                failedJob.fail(exception.getMessage());
                analysisJobRepository.saveAndFlush(failedJob);
            }

            throw exception;
        }
    }

    private ConnectedRepository getOwnedConnectedRepository(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return connectedRepositoryRepository
                .findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "연결된 저장소를 찾을 수 없습니다."
                        )
                );
    }

    public AnalysisJob getJob(
            Long userId,
            Long connectedRepositoryId,
            Long analysisJobId
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        return analysisJobRepository
                .findByIdAndConnectedRepository(
                        analysisJobId,
                        connectedRepository
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "분석 작업을 찾을 수 없습니다."
                        )
                );
    }

    public List<AnalysisJob> getJobs(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        if (targetDate == null) {
            return analysisJobRepository
                    .findAllByConnectedRepositoryOrderByCreatedAtDesc(
                            connectedRepository
                    );
        }

        return analysisJobRepository
                .findAllByConnectedRepositoryAndTargetDateOrderByCreatedAtDesc(
                        connectedRepository,
                        targetDate
                );
    }
}