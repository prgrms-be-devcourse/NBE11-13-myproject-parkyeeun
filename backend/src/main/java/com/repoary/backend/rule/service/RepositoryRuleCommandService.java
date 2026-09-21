package com.repoary.backend.rule.service;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.exception.RepositoryErrorCode;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import com.repoary.backend.rule.dto.ClassificationRuleRequest;
import com.repoary.backend.rule.dto.ClassificationRuleResponse;
import com.repoary.backend.rule.dto.ConventionRuleRequest;
import com.repoary.backend.rule.dto.ConventionRuleResponse;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.rule.exception.RuleErrorCode;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.exception.UserErrorCode;
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
                        new BusinessException(UserErrorCode.USER_NOT_FOUND)
                );

        return connectedRepositoryRepository
                .findByIdAndUser(connectedRepositoryId, user)
                .orElseThrow(() ->
                        new BusinessException(
                                RepositoryErrorCode.CONNECTED_REPOSITORY_NOT_FOUND
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
                        new BusinessException(
                                RuleErrorCode.CLASSIFICATION_RULE_NOT_FOUND
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
                        new BusinessException(
                                RuleErrorCode.CONVENTION_RULE_NOT_FOUND
                        )
                );
    }

    private void validateClassificationRequest(
            ClassificationRuleRequest request
    ) {
        if (request.pathPattern() == null
                || request.pathPattern().isBlank()) {
            throw new BusinessException(RuleErrorCode.PATH_PATTERN_REQUIRED);
        }

        if (request.category() == null
                || request.category().isBlank()) {
            throw new BusinessException(RuleErrorCode.CATEGORY_REQUIRED);
        }

        validatePriority(request.priority());
    }

    private void validateConventionRequest(
            ConventionRuleRequest request
    ) {
        if (request.messagePattern() == null
                || request.messagePattern().isBlank()) {
            throw new BusinessException(RuleErrorCode.MESSAGE_PATTERN_REQUIRED);
        }

        boolean hasResult =
                hasText(request.commitType())
                        || hasText(request.scope())
                        || hasText(request.category());

        if (!hasResult) {
            throw new BusinessException(RuleErrorCode.MATCH_CONDITION_REQUIRED);
        }

        validatePriority(request.priority());
    }

    private void validatePriority(Integer priority) {
        if (priority != null && priority < 0) {
            throw new BusinessException(RuleErrorCode.INVALID_PRIORITY);
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
            throw new BusinessException(RuleErrorCode.DUPLICATE_PATH_PATTERN);
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
            throw new BusinessException(RuleErrorCode.DUPLICATE_MESSAGE_PATTERN);
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
