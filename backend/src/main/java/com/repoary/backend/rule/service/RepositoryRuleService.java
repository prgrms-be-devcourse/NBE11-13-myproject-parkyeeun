package com.repoary.backend.rule.service;

import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import com.repoary.backend.rule.preset.DefaultRulePreset;
import com.repoary.backend.rule.preset.DefaultRulePreset.ConventionRulePreset;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RepositoryRuleService {

    private final GitHubApiClient gitHubApiClient;
    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ClassificationRuleRepository classificationRuleRepository;
    private final ConventionRuleRepository conventionRuleRepository;

    public RepositoryRuleService(
            GitHubApiClient gitHubApiClient,
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            ClassificationRuleRepository classificationRuleRepository,
            ConventionRuleRepository conventionRuleRepository
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.classificationRuleRepository = classificationRuleRepository;
        this.conventionRuleRepository = conventionRuleRepository;
    }

    @Transactional
    public void createMissingDefaultRules(
            User user,
            ConnectedRepository connectedRepository
    ) {
        String accessToken = user.getGithubAccessToken();

        validateGitHubAccessToken(accessToken);

        createMissingClassificationRules(
                accessToken,
                connectedRepository
        );

        createMissingConventionRules(connectedRepository);
    }

    @Transactional
    public void restoreDefaultRules(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        ConnectedRepository connectedRepository =
                connectedRepositoryRepository
                        .findByIdAndUser(
                                connectedRepositoryId,
                                user
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "연결된 저장소를 찾을 수 없습니다."
                                )
                        );

        createMissingDefaultRules(
                user,
                connectedRepository
        );
    }

    private void createMissingClassificationRules(
            String accessToken,
            ConnectedRepository connectedRepository
    ) {
        RepositoryIdentifier identifier =
                RepositoryIdentifier.from(
                        connectedRepository.getFullName()
                );

        List<String> rootDirectoryNames =
                gitHubApiClient.getRootDirectoryNames(
                        accessToken,
                        identifier.owner(),
                        identifier.repositoryName(),
                        connectedRepository.getDefaultBranch()
                );

        List<ClassificationRule> newRules = rootDirectoryNames.stream()
                .filter(DefaultRulePreset::supportsDirectory)
                .filter(directoryName ->
                        !classificationRuleRepository
                                .existsByConnectedRepositoryAndPathPattern(
                                        connectedRepository,
                                        toPathPattern(directoryName)
                                )
                )
                .map(directoryName -> new ClassificationRule(
                        connectedRepository,
                        toPathPattern(directoryName),
                        DefaultRulePreset.getCategory(directoryName),
                        null,
                        DefaultRulePreset.DEFAULT_PRIORITY,
                        true
                ))
                .toList();

        classificationRuleRepository.saveAll(newRules);
    }

    private void createMissingConventionRules(
            ConnectedRepository connectedRepository
    ) {
        List<ConventionRule> newRules =
                DefaultRulePreset.getConventionRules()
                        .stream()
                        .filter(preset ->
                                !conventionRuleRepository
                                        .existsByConnectedRepositoryAndMessagePattern(
                                                connectedRepository,
                                                preset.messagePattern()
                                        )
                        )
                        .map(preset -> toConventionRule(
                                connectedRepository,
                                preset
                        ))
                        .toList();

        conventionRuleRepository.saveAll(newRules);
    }

    private ConventionRule toConventionRule(
            ConnectedRepository connectedRepository,
            ConventionRulePreset preset
    ) {
        return new ConventionRule(
                connectedRepository,
                preset.messagePattern(),
                preset.commitType(),
                preset.scope(),
                preset.category(),
                DefaultRulePreset.DEFAULT_PRIORITY,
                true
        );
    }

    private String toPathPattern(String directoryName) {
        return directoryName + "/**";
    }

    private void validateGitHubAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "GitHub access token을 찾을 수 없습니다."
            );
        }
    }

    private record RepositoryIdentifier(
            String owner,
            String repositoryName
    ) {

        private static RepositoryIdentifier from(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException(
                        "GitHub 저장소 전체 이름이 올바르지 않습니다."
                );
            }

            String[] parts = fullName.split("/", 2);

            if (parts.length != 2
                    || parts[0].isBlank()
                    || parts[1].isBlank()) {
                throw new IllegalArgumentException(
                        "GitHub 저장소 전체 이름은 owner/repository 형식이어야 합니다."
                );
            }

            return new RepositoryIdentifier(
                    parts[0],
                    parts[1]
            );
        }
    }
}