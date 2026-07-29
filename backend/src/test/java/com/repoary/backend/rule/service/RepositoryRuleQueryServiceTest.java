package com.repoary.backend.rule.service;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.rule.domain.ClassificationRule;
import com.repoary.backend.rule.domain.ConventionRule;
import com.repoary.backend.rule.dto.ClassificationRuleResponse;
import com.repoary.backend.rule.dto.ConventionRuleResponse;
import com.repoary.backend.rule.repository.ClassificationRuleRepository;
import com.repoary.backend.rule.repository.ConventionRuleRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryRuleQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectedRepositoryRepository connectedRepositoryRepository;

    @Mock
    private ClassificationRuleRepository classificationRuleRepository;

    @Mock
    private ConventionRuleRepository conventionRuleRepository;

    @InjectMocks
    private RepositoryRuleQueryService repositoryRuleQueryService;

    private User user;
    private ConnectedRepository connectedRepository;

    @BeforeEach
    void setUp() {
        user = new User(
                100L,
                "repoary-user"
        );

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
    @DisplayName("경로 규칙을 우선순위 순으로 조회한다")
    void getClassificationRules() {
        ClassificationRule highPriorityRule = new ClassificationRule(
                connectedRepository,
                "docs/spring/**",
                "lectures",
                "spring",
                50,
                false
        );

        ClassificationRule defaultRule = new ClassificationRule(
                connectedRepository,
                "assignments/**",
                "assignments",
                null,
                100,
                true
        );

        mockOwnedRepository();

        when(classificationRuleRepository
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                ))
                .thenReturn(List.of(
                        highPriorityRule,
                        defaultRule
                ));

        List<ClassificationRuleResponse> responses =
                repositoryRuleQueryService.getClassificationRules(
                        1L,
                        11L
                );

        assertThat(responses)
                .hasSize(2);

        assertThat(responses)
                .extracting(ClassificationRuleResponse::pathPattern)
                .containsExactly(
                        "docs/spring/**",
                        "assignments/**"
                );

        assertThat(responses)
                .extracting(ClassificationRuleResponse::priority)
                .containsExactly(
                        50,
                        100
                );

        assertThat(responses.get(0).defaultRule())
                .isFalse();

        assertThat(responses.get(1).defaultRule())
                .isTrue();
    }

    @Test
    @DisplayName("비활성화된 경로 규칙도 관리용 전체 조회에 포함한다")
    void getClassificationRulesIncludesDisabledRules() {
        ClassificationRule disabledRule = new ClassificationRule(
                connectedRepository,
                "docs/java/**",
                "lectures",
                "java",
                50,
                false
        );

        disabledRule.updateEnabled(false);

        mockOwnedRepository();

        when(classificationRuleRepository
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                ))
                .thenReturn(List.of(disabledRule));

        List<ClassificationRuleResponse> responses =
                repositoryRuleQueryService.getClassificationRules(
                        1L,
                        11L
                );

        assertThat(responses)
                .hasSize(1);

        assertThat(responses.get(0).enabled())
                .isFalse();
    }

    @Test
    @DisplayName("커밋 규칙을 우선순위 순으로 조회한다")
    void getConventionRules() {
        ConventionRule highPriorityRule = new ConventionRule(
                connectedRepository,
                "feat(api):",
                "feat",
                "api",
                "project",
                50,
                false
        );

        ConventionRule defaultRule = new ConventionRule(
                connectedRepository,
                "study(java):",
                "study",
                "java",
                null,
                100,
                true
        );

        mockOwnedRepository();

        when(conventionRuleRepository
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                ))
                .thenReturn(List.of(
                        highPriorityRule,
                        defaultRule
                ));

        List<ConventionRuleResponse> responses =
                repositoryRuleQueryService.getConventionRules(
                        1L,
                        11L
                );

        assertThat(responses)
                .hasSize(2);

        assertThat(responses)
                .extracting(ConventionRuleResponse::messagePattern)
                .containsExactly(
                        "feat(api):",
                        "study(java):"
                );

        assertThat(responses)
                .extracting(ConventionRuleResponse::priority)
                .containsExactly(
                        50,
                        100
                );

        assertThat(responses.get(0).commitType())
                .isEqualTo("feat");

        assertThat(responses.get(0).scope())
                .isEqualTo("api");

        assertThat(responses.get(1).defaultRule())
                .isTrue();
    }

    @Test
    @DisplayName("비활성화된 커밋 규칙도 관리용 전체 조회에 포함한다")
    void getConventionRulesIncludesDisabledRules() {
        ConventionRule disabledRule = new ConventionRule(
                connectedRepository,
                "feat(api):",
                "feat",
                "api",
                "project",
                50,
                false
        );

        disabledRule.updateEnabled(false);

        mockOwnedRepository();

        when(conventionRuleRepository
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                ))
                .thenReturn(List.of(disabledRule));

        List<ConventionRuleResponse> responses =
                repositoryRuleQueryService.getConventionRules(
                        1L,
                        11L
                );

        assertThat(responses)
                .hasSize(1);

        assertThat(responses.get(0).enabled())
                .isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 규칙은 조회할 수 없다")
    void failWhenUserDoesNotExist() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                repositoryRuleQueryService.getClassificationRules(
                        1L,
                        11L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(connectedRepositoryRepository, never())
                .findByIdAndUser(
                        11L,
                        user
                );

        verify(classificationRuleRepository, never())
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                );
    }

    @Test
    @DisplayName("다른 사용자의 연결 저장소 규칙은 조회할 수 없다")
    void failWhenConnectedRepositoryIsNotOwnedByUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(connectedRepositoryRepository.findByIdAndUser(
                11L,
                user
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                repositoryRuleQueryService.getConventionRules(
                        1L,
                        11L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("연결된 저장소를 찾을 수 없습니다.");

        verify(conventionRuleRepository, never())
                .findAllByConnectedRepositoryOrderByPriorityAsc(
                        connectedRepository
                );
    }

    private void mockOwnedRepository() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(connectedRepositoryRepository.findByIdAndUser(
                11L,
                user
        )).thenReturn(Optional.of(connectedRepository));
    }
}