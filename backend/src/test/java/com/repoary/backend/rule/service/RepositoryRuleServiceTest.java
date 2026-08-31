package com.repoary.backend.rule.service;

import com.repoary.backend.github.client.GitHubApiClient;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryRuleServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectedRepositoryRepository connectedRepositoryRepository;

    @Mock
    private ClassificationRuleRepository classificationRuleRepository;

    @Mock
    private ConventionRuleRepository conventionRuleRepository;

    @InjectMocks
    private RepositoryRuleService repositoryRuleService;

    private User user;
    private ConnectedRepository connectedRepository;

    @BeforeEach
    void setUp() {
        user = new User(
                100L,
                "repoary-user"
        );
        user.updateGitHubAccessToken("github-access-token");

        connectedRepository = new ConnectedRepository(
                user,
                200L,
                "programmers-devcourse-be11",
                "dPdms21/programmers-devcourse-be11",
                "https://github.com/dPdms21/programmers-devcourse-be11",
                false,
                "main"
        );
    }

    @Test
    @DisplayName("저장소 루트에 존재하는 지원 디렉터리와 기본 커밋 규칙을 생성한다")
    void createMissingDefaultRules() {
        when(gitHubApiClient.getRootDirectoryNames(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main"
        )).thenReturn(List.of(
                "assignments",
                "codingtest",
                "docs",
                "lectures",
                "practice",
                "src",
                "til"
        ));

        repositoryRuleService.createMissingDefaultRules(
                user,
                connectedRepository
        );

        ArgumentCaptor<Iterable<ClassificationRule>> classificationCaptor =
                iterableCaptor();

        verify(classificationRuleRepository)
                .saveAll(classificationCaptor.capture());

        List<ClassificationRule> classificationRules =
                toList(classificationCaptor.getValue());

        assertThat(classificationRules)
                .hasSize(5)
                .extracting(ClassificationRule::getPathPattern)
                .containsExactlyInAnyOrder(
                        "assignments/**",
                        "codingtest/**",
                        "lectures/**",
                        "practice/**",
                        "til/**"
                );

        assertThat(classificationRules)
                .allSatisfy(rule -> {
                    assertThat(rule.getPriority()).isEqualTo(100);
                    assertThat(rule.isEnabled()).isTrue();
                    assertThat(rule.isDefaultRule()).isTrue();
                });

        ArgumentCaptor<Iterable<ConventionRule>> conventionCaptor =
                iterableCaptor();

        verify(conventionRuleRepository)
                .saveAll(conventionCaptor.capture());

        List<ConventionRule> conventionRules =
                toList(conventionCaptor.getValue());

        assertThat(conventionRules)
                .hasSize(14)
                .extracting(ConventionRule::getMessagePattern)
                .contains(
                        "docs(assignments):",
                        "docs(lectures):",
                        "study(java):",
                        "solve(java):",
                        "chore(project):"
                );

        assertThat(conventionRules)
                .allSatisfy(rule -> {
                    assertThat(rule.getPriority()).isEqualTo(100);
                    assertThat(rule.isEnabled()).isTrue();
                    assertThat(rule.isDefaultRule()).isTrue();
                });
    }

    @Test
    @DisplayName("기본 규칙 복원 시 삭제된 규칙만 다시 생성한다")
    void restoreDefaultRulesCreatesOnlyMissingRules() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(connectedRepositoryRepository.findByIdAndUser(
                11L,
                user
        )).thenReturn(Optional.of(connectedRepository));

        when(gitHubApiClient.getRootDirectoryNames(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main"
        )).thenReturn(List.of(
                "assignments",
                "codingtest",
                "lectures",
                "practice",
                "til"
        ));

        when(classificationRuleRepository
                .existsByConnectedRepositoryAndPathPattern(
                        eq(connectedRepository),
                        anyString()
                ))
                .thenAnswer(invocation ->
                        !"assignments/**".equals(
                                invocation.getArgument(1, String.class)
                        )
                );

        when(conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePattern(
                        eq(connectedRepository),
                        anyString()
                ))
                .thenAnswer(invocation ->
                        !"docs(assignments):".equals(
                                invocation.getArgument(1, String.class)
                        )
                );

        repositoryRuleService.restoreDefaultRules(
                1L,
                11L
        );

        ArgumentCaptor<Iterable<ClassificationRule>> classificationCaptor =
                iterableCaptor();

        verify(classificationRuleRepository)
                .saveAll(classificationCaptor.capture());

        List<ClassificationRule> classificationRules =
                toList(classificationCaptor.getValue());

        assertThat(classificationRules)
                .hasSize(1);

        assertThat(classificationRules.get(0).getPathPattern())
                .isEqualTo("assignments/**");

        assertThat(classificationRules.get(0).isDefaultRule())
                .isTrue();

        ArgumentCaptor<Iterable<ConventionRule>> conventionCaptor =
                iterableCaptor();

        verify(conventionRuleRepository)
                .saveAll(conventionCaptor.capture());

        List<ConventionRule> conventionRules =
                toList(conventionCaptor.getValue());

        assertThat(conventionRules)
                .hasSize(1);

        assertThat(conventionRules.get(0).getMessagePattern())
                .isEqualTo("docs(assignments):");

        assertThat(conventionRules.get(0).isDefaultRule())
                .isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 기본 규칙은 중복 생성하지 않는다")
    void createMissingDefaultRulesSkipsExistingRules() {
        when(gitHubApiClient.getRootDirectoryNames(
                "github-access-token",
                "dPdms21",
                "programmers-devcourse-be11",
                "main"
        )).thenReturn(List.of(
                "assignments",
                "codingtest",
                "lectures",
                "practice",
                "til"
        ));

        when(classificationRuleRepository
                .existsByConnectedRepositoryAndPathPattern(
                        eq(connectedRepository),
                        anyString()
                ))
                .thenReturn(true);

        when(conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePattern(
                        eq(connectedRepository),
                        anyString()
                ))
                .thenReturn(true);

        repositoryRuleService.createMissingDefaultRules(
                user,
                connectedRepository
        );

        ArgumentCaptor<Iterable<ClassificationRule>> classificationCaptor =
                iterableCaptor();

        verify(classificationRuleRepository)
                .saveAll(classificationCaptor.capture());

        assertThat(toList(classificationCaptor.getValue()))
                .isEmpty();

        ArgumentCaptor<Iterable<ConventionRule>> conventionCaptor =
                iterableCaptor();

        verify(conventionRuleRepository)
                .saveAll(conventionCaptor.capture());

        assertThat(toList(conventionCaptor.getValue()))
                .isEmpty();
    }

    @Test
    @DisplayName("GitHub access token이 없으면 기본 규칙을 생성하지 않는다")
    void createMissingDefaultRulesWithoutAccessToken() {
        User userWithoutToken = new User(
                100L,
                "repoary-user"
        );

        assertThatThrownBy(() ->
                repositoryRuleService.createMissingDefaultRules(
                        userWithoutToken,
                        connectedRepository
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GitHub access token을 찾을 수 없습니다.");

        verify(gitHubApiClient, never())
                .getRootDirectoryNames(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                );

        verify(classificationRuleRepository, never())
                .saveAll(org.mockito.ArgumentMatchers.any());

        verify(conventionRuleRepository, never())
                .saveAll(org.mockito.ArgumentMatchers.any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Iterable<T>> iterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }

    private static <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();

        values.forEach(result::add);

        return result;
    }
}