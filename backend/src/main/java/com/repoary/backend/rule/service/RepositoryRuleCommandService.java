package com.repoary.backend.rule.service;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import com.repoary.backend.rule.dto.ClassificationRuleRequest;
import com.repoary.backend.rule.dto.ClassificationRuleResponse;
import com.repoary.backend.rule.dto.ConventionRuleRequest;
import com.repoary.backend.rule.dto.ConventionRuleResponse;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryRuleCommandService {

    private static final int DEFAULT_PRIORITY = 100;

    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ConventionRuleRepository conventionRuleRepository;

    public RepositoryRuleCommandService(
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

    @Transactional
    public ClassificationRuleResponse createClassificationRule(
            Long userId,
            Long connectedRepositoryId,
            ClassificationRuleRequest request
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        validateClassificationRequest(request);
        validateClassificationPatternDuplicate(
                connectedRepository,
                request.pathPattern(),
                null
        );

        ClassificationRule rule = new ClassificationRule(
                connectedRepository,
                request.pathPattern().trim(),
                request.category().trim(),
                trimToNull(request.scope()),
                resolvePriority(request.priority()),
                false
        );

        return ClassificationRuleResponse.from(
                classificationRuleRepository.save(rule)
        );
    }

    @Transactional
    public ClassificationRuleResponse updateClassificationRule(
            Long userId,
            Long connectedRepositoryId,
            Long ruleId,
            ClassificationRuleRequest request
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        ClassificationRule rule = getClassificationRule(
                connectedRepository,
                ruleId
        );

        validateClassificationRequest(request);
        validateClassificationPatternDuplicate(
                connectedRepository,
                request.pathPattern(),
                ruleId
        );

        rule.update(
                request.pathPattern().trim(),
                request.category().trim(),
                trimToNull(request.scope()),
                resolvePriority(request.priority())
        );

        return ClassificationRuleResponse.from(rule);
    }

    @Transactional
    public ClassificationRuleResponse updateClassificationEnabled(
            Long userId,
            Long connectedRepositoryId,
            Long ruleId,
            boolean enabled
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        ClassificationRule rule = getClassificationRule(
                connectedRepository,
                ruleId
        );

        rule.updateEnabled(enabled);

        return ClassificationRuleResponse.from(rule);
    }

    @Transactional
    public void deleteClassificationRule(
            Long userId,
            Long connectedRepositoryId,
            Long ruleId
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        ClassificationRule rule = getClassificationRule(
                connectedRepository,
                ruleId
        );

        classificationRuleRepository.delete(rule);
    }

    @Transactional
    public ConventionRuleResponse createConventionRule(
            Long userId,
            Long connectedRepositoryId,
            ConventionRuleRequest request
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        validateConventionRequest(request);
        validateConventionPatternDuplicate(
                connectedRepository,
                request.messagePattern(),
                null
        );

        ConventionRule rule = new ConventionRule(
                connectedRepository,
                request.messagePattern().trim(),
                trimToNull(request.commitType()),
                trimToNull(request.scope()),
                trimToNull(request.category()),
                resolvePriority(request.priority()),
                false
        );

        return ConventionRuleResponse.from(
                conventionRuleRepository.save(rule)
        );
    }

    @Transactional
    public ConventionRuleResponse updateConventionRule(
            Long userId,
            Long connectedRepositoryId,
            Long ruleId,
            ConventionRuleRequest request
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        ConventionRule rule = getConventionRule(
                connectedRepository,
                ruleId
        );

        validateConventionRequest(request);
        validateConventionPatternDuplicate(
                connectedRepository,
                request.messagePattern(),
                ruleId
        );

        rule.update(
                request.messagePattern().trim(),
                trimToNull(request.commitType()),
                trimToNull(request.scope()),
                trimToNull(request.category()),
                resolvePriority(request.priority())
        );

        return ConventionRuleResponse.from(rule);
    }

    @Transactional
    public ConventionRuleResponse updateConventionEnabled(
            Long userId,
            Long connectedRepositoryId,
            Long ruleId,
            boolean enabled
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        ConventionRule rule = getConventionRule(
                connectedRepository,
                ruleId
        );

        rule.updateEnabled(enabled);

        return ConventionRuleResponse.from(rule);
    }

    @Transactional
    public void deleteConventionRule(
            Long userId,
            Long connectedRepositoryId,
            Long ruleId
    ) {
        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(userId, connectedRepositoryId);

        ConventionRule rule = getConventionRule(
                connectedRepository,
                ruleId
        );

        conventionRuleRepository.delete(rule);
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

    private ClassificationRule getClassificationRule(
            ConnectedRepository connectedRepository,
            Long ruleId
    ) {
        return classificationRuleRepository
                .findByIdAndConnectedRepository(
                        ruleId,
                        connectedRepository
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "경로 규칙을 찾을 수 없습니다."
                        )
                );
    }

    private ConventionRule getConventionRule(
            ConnectedRepository connectedRepository,
            Long ruleId
    ) {
        return conventionRuleRepository
                .findByIdAndConnectedRepository(
                        ruleId,
                        connectedRepository
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "커밋 규칙을 찾을 수 없습니다."
                        )
                );
    }

    private void validateClassificationRequest(
            ClassificationRuleRequest request
    ) {
        if (request.pathPattern() == null
                || request.pathPattern().isBlank()) {
            throw new IllegalArgumentException(
                    "경로 패턴은 필수입니다."
            );
        }

        if (request.category() == null
                || request.category().isBlank()) {
            throw new IllegalArgumentException(
                    "카테고리는 필수입니다."
            );
        }

        validatePriority(request.priority());
    }

    private void validateConventionRequest(
            ConventionRuleRequest request
    ) {
        if (request.messagePattern() == null
                || request.messagePattern().isBlank()) {
            throw new IllegalArgumentException(
                    "커밋 메시지 패턴은 필수입니다."
            );
        }

        boolean hasResult =
                hasText(request.commitType())
                        || hasText(request.scope())
                        || hasText(request.category());

        if (!hasResult) {
            throw new IllegalArgumentException(
                    "commitType, scope, category 중 하나 이상은 필요합니다."
            );
        }

        validatePriority(request.priority());
    }

    private void validatePriority(Integer priority) {
        if (priority != null && priority < 0) {
            throw new IllegalArgumentException(
                    "우선순위는 0 이상이어야 합니다."
            );
        }
    }

    private void validateClassificationPatternDuplicate(
            ConnectedRepository connectedRepository,
            String pathPattern,
            Long excludedRuleId
    ) {
        String normalizedPattern = pathPattern.trim();

        boolean exists = excludedRuleId == null
                ? classificationRuleRepository
                .existsByConnectedRepositoryAndPathPattern(
                        connectedRepository,
                        normalizedPattern
                )
                : classificationRuleRepository
                .existsByConnectedRepositoryAndPathPatternAndIdNot(
                        connectedRepository,
                        normalizedPattern,
                        excludedRuleId
                );

        if (exists) {
            throw new IllegalArgumentException(
                    "이미 존재하는 경로 패턴입니다."
            );
        }
    }

    private void validateConventionPatternDuplicate(
            ConnectedRepository connectedRepository,
            String messagePattern,
            Long excludedRuleId
    ) {
        String normalizedPattern = messagePattern.trim();

        boolean exists = excludedRuleId == null
                ? conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePattern(
                        connectedRepository,
                        normalizedPattern
                )
                : conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePatternAndIdNot(
                        connectedRepository,
                        normalizedPattern,
                        excludedRuleId
                );

        if (exists) {
            throw new IllegalArgumentException(
                    "이미 존재하는 커밋 메시지 패턴입니다."
            );
        }
    }

    private int resolvePriority(Integer priority) {
        return priority == null
                ? DEFAULT_PRIORITY
                : priority;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}