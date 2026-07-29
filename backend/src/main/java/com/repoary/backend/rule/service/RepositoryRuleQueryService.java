package com.repoary.backend.rule.service;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.dto.ClassificationRuleResponse;
import com.repoary.backend.rule.dto.ConventionRuleResponse;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RepositoryRuleQueryService {

    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ConventionRuleRepository conventionRuleRepository;

    public RepositoryRuleQueryService(
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            ClassificationRuleRepository classificationRuleRepository,
            ConventionRuleRepository conventionRuleRepository
    ) {
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.classificationRuleRepository = classificationRuleRepository;
        this.conventionRuleRepository = conventionRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassificationRuleResponse> getClassificationRules(
            Long userId,
            Long connectedRepositoryId
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        return classificationRuleRepository
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                )
                .stream()
                .map(ClassificationRuleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConventionRuleResponse> getConventionRules(
            Long userId,
            Long connectedRepositoryId
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        return conventionRuleRepository
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                )
                .stream()
                .map(ConventionRuleResponse::from)
                .toList();
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
                .findByIdAndUser(connectedRepositoryId, user)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "연결된 저장소를 찾을 수 없습니다."
                        )
                );
    }
}