package com.repoary.backend.analysis.service;

import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import tools.jackson.databind.json.JsonMapper;
import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.dto.CommitAnalysisResponse;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.analysis.repository.AnalysisJobRepository;
import com.repoary.backend.analysis.exception.AnalysisErrorCode;
import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.common.exception.CommonErrorCode;
import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.exception.RepositoryErrorCode;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.exception.UserErrorCode;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalysisJobService {

    private static final Logger log = LoggerFactory.getLogger(
            AnalysisJobService.class
    );

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
            throw new BusinessException(AnalysisErrorCode.DATE_REQUIRED);
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
                failedJob.fail(getSafeFailureMessage(exception));
                analysisJobRepository.saveAndFlush(failedJob);
            }

            log.error(
                    "Analysis job failed. jobId={}, exceptionType={}, causeType={}",
                    analysisJob.getId(),
                    exception.getClass().getName(),
                    exception.getCause() == null
                            ? "none"
                            : exception.getCause().getClass().getName()
            );

            throw exception;
        }
    }

    private String getSafeFailureMessage(Exception exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getErrorCode().getMessage();
        }

        if (exception instanceof ExternalSystemException externalException) {
            return externalException.getErrorCode().getMessage();
        }

        return CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage();
    }

    private ConnectedRepository getOwnedConnectedRepository(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(UserErrorCode.USER_NOT_FOUND)
                );

        return connectedRepositoryRepository
                .findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
                .orElseThrow(() ->
                        new BusinessException(
                                RepositoryErrorCode.CONNECTED_REPOSITORY_NOT_FOUND
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
                        new BusinessException(AnalysisErrorCode.JOB_NOT_FOUND)
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
