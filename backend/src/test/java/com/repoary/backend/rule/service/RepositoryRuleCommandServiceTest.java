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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryRuleCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectedRepositoryRepository connectedRepositoryRepository;

    @Mock
    private ClassificationRuleRepository classificationRuleRepository;

    @Mock
    private ConventionRuleRepository conventionRuleRepository;

    @InjectMocks
    private RepositoryRuleCommandService repositoryRuleCommandService;

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
    @DisplayName("사용자 경로 규칙을 생성한다")
    void createClassificationRule() {
        ClassificationRuleRequest request =
                new ClassificationRuleRequest(
                        "docs/spring/**",
                        "lectures",
                        "spring",
                        50
                );

        mockOwnedRepository();

        when(classificationRuleRepository
                .existsByConnectedRepositoryAndPathPattern(
                        connectedRepository,
                        "docs/spring/**"
                ))
                .thenReturn(false);

        when(classificationRuleRepository.save(any(ClassificationRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClassificationRuleResponse response =
                repositoryRuleCommandService.createClassificationRule(
                        1L,
                        11L,
                        request
                );

        ArgumentCaptor<ClassificationRule> ruleCaptor =
                ArgumentCaptor.forClass(ClassificationRule.class);

        verify(classificationRuleRepository)
                .save(ruleCaptor.capture());

        ClassificationRule savedRule = ruleCaptor.getValue();

        assertThat(savedRule.getPathPattern())
                .isEqualTo("docs/spring/**");
        assertThat(savedRule.getCategory())
                .isEqualTo("lectures");
        assertThat(savedRule.getScope())
                .isEqualTo("spring");
        assertThat(savedRule.getPriority())
                .isEqualTo(50);
        assertThat(savedRule.isEnabled())
                .isTrue();
        assertThat(savedRule.isDefaultRule())
                .isFalse();

        assertThat(response.pathPattern())
                .isEqualTo("docs/spring/**");
        assertThat(response.defaultRule())
                .isFalse();
    }

    @Test
    @DisplayName("경로 규칙의 priority가 없으면 기본값 100을 적용한다")
    void createClassificationRuleWithDefaultPriority() {
        ClassificationRuleRequest request =
                new ClassificationRuleRequest(
                        "docs/java/**",
                        "lectures",
                        "java",
                        null
                );

        mockOwnedRepository();

        when(classificationRuleRepository
                .existsByConnectedRepositoryAndPathPattern(
                        connectedRepository,
                        "docs/java/**"
                ))
                .thenReturn(false);

        when(classificationRuleRepository.save(any(ClassificationRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClassificationRuleResponse response =
                repositoryRuleCommandService.createClassificationRule(
                        1L,
                        11L,
                        request
                );

        assertThat(response.priority())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("이미 존재하는 경로 패턴은 생성할 수 없다")
    void createClassificationRuleWithDuplicatePattern() {
        ClassificationRuleRequest request =
                new ClassificationRuleRequest(
                        "assignments/**",
                        "assignments",
                        null,
                        100
                );

        mockOwnedRepository();

        when(classificationRuleRepository
                .existsByConnectedRepositoryAndPathPattern(
                        connectedRepository,
                        "assignments/**"
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                repositoryRuleCommandService.createClassificationRule(
                        1L,
                        11L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 경로 패턴입니다.");

        verify(classificationRuleRepository, never())
                .save(any(ClassificationRule.class));
    }

    @Test
    @DisplayName("경로 규칙의 priority는 0 이상이어야 한다")
    void createClassificationRuleWithNegativePriority() {
        ClassificationRuleRequest request =
                new ClassificationRuleRequest(
                        "invalid/**",
                        "test",
                        null,
                        -1
                );

        mockOwnedRepository();

        assertThatThrownBy(() ->
                repositoryRuleCommandService.createClassificationRule(
                        1L,
                        11L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("우선순위는 0 이상이어야 합니다.");

        verify(classificationRuleRepository, never())
                .save(any(ClassificationRule.class));
    }

    @Test
    @DisplayName("경로 규칙을 수정한다")
    void updateClassificationRule() {
        ClassificationRule rule = new ClassificationRule(
                connectedRepository,
                "docs/spring/**",
                "lectures",
                "spring",
                50,
                false
        );

        ClassificationRuleRequest request =
                new ClassificationRuleRequest(
                        "docs/springboot/**",
                        "lectures",
                        "springboot",
                        40
                );

        mockOwnedRepository();

        when(classificationRuleRepository
                .findByIdAndConnectedRepository(
                        1L,
                        connectedRepository
                ))
                .thenReturn(Optional.of(rule));

        when(classificationRuleRepository
                .existsByConnectedRepositoryAndPathPatternAndIdNot(
                        connectedRepository,
                        "docs/springboot/**",
                        1L
                ))
                .thenReturn(false);

        ClassificationRuleResponse response =
                repositoryRuleCommandService.updateClassificationRule(
                        1L,
                        11L,
                        1L,
                        request
                );

        assertThat(response.pathPattern())
                .isEqualTo("docs/springboot/**");
        assertThat(response.scope())
                .isEqualTo("springboot");
        assertThat(response.priority())
                .isEqualTo(40);
        assertThat(response.defaultRule())
                .isFalse();
    }

    @Test
    @DisplayName("경로 규칙을 비활성화한다")
    void disableClassificationRule() {
        ClassificationRule rule = new ClassificationRule(
                connectedRepository,
                "docs/spring/**",
                "lectures",
                "spring",
                50,
                false
        );

        mockOwnedRepository();

        when(classificationRuleRepository
                .findByIdAndConnectedRepository(
                        1L,
                        connectedRepository
                ))
                .thenReturn(Optional.of(rule));

        ClassificationRuleResponse response =
                repositoryRuleCommandService.updateClassificationEnabled(
                        1L,
                        11L,
                        1L,
                        false
                );

        assertThat(response.enabled())
                .isFalse();
    }

    @Test
    @DisplayName("경로 규칙을 삭제한다")
    void deleteClassificationRule() {
        ClassificationRule rule = new ClassificationRule(
                connectedRepository,
                "docs/spring/**",
                "lectures",
                "spring",
                50,
                false
        );

        mockOwnedRepository();

        when(classificationRuleRepository
                .findByIdAndConnectedRepository(
                        1L,
                        connectedRepository
                ))
                .thenReturn(Optional.of(rule));

        repositoryRuleCommandService.deleteClassificationRule(
                1L,
                11L,
                1L
        );

        verify(classificationRuleRepository)
                .delete(rule);
    }

    @Test
    @DisplayName("사용자 커밋 규칙을 생성한다")
    void createConventionRule() {
        ConventionRuleRequest request =
                new ConventionRuleRequest(
                        "feat(api):",
                        "feat",
                        "api",
                        "project",
                        50
                );

        mockOwnedRepository();

        when(conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePattern(
                        connectedRepository,
                        "feat(api):"
                ))
                .thenReturn(false);

        when(conventionRuleRepository.save(any(ConventionRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConventionRuleResponse response =
                repositoryRuleCommandService.createConventionRule(
                        1L,
                        11L,
                        request
                );

        ArgumentCaptor<ConventionRule> ruleCaptor =
                ArgumentCaptor.forClass(ConventionRule.class);

        verify(conventionRuleRepository)
                .save(ruleCaptor.capture());

        ConventionRule savedRule = ruleCaptor.getValue();

        assertThat(savedRule.getMessagePattern())
                .isEqualTo("feat(api):");
        assertThat(savedRule.getCommitType())
                .isEqualTo("feat");
        assertThat(savedRule.getScope())
                .isEqualTo("api");
        assertThat(savedRule.getCategory())
                .isEqualTo("project");
        assertThat(savedRule.getPriority())
                .isEqualTo(50);
        assertThat(savedRule.isEnabled())
                .isTrue();
        assertThat(savedRule.isDefaultRule())
                .isFalse();

        assertThat(response.messagePattern())
                .isEqualTo("feat(api):");
    }

    @Test
    @DisplayName("커밋 규칙은 결과값을 하나 이상 가져야 한다")
    void createConventionRuleWithoutResult() {
        ConventionRuleRequest request =
                new ConventionRuleRequest(
                        "test:",
                        null,
                        null,
                        null,
                        100
                );

        mockOwnedRepository();

        assertThatThrownBy(() ->
                repositoryRuleCommandService.createConventionRule(
                        1L,
                        11L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "commitType, scope, category 중 하나 이상은 필요합니다."
                );

        verify(conventionRuleRepository, never())
                .save(any(ConventionRule.class));
    }

    @Test
    @DisplayName("이미 존재하는 커밋 메시지 패턴은 생성할 수 없다")
    void createConventionRuleWithDuplicatePattern() {
        ConventionRuleRequest request =
                new ConventionRuleRequest(
                        "study(java):",
                        "study",
                        "java",
                        null,
                        100
                );

        mockOwnedRepository();

        when(conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePattern(
                        connectedRepository,
                        "study(java):"
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                repositoryRuleCommandService.createConventionRule(
                        1L,
                        11L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "이미 존재하는 커밋 메시지 패턴입니다."
                );

        verify(conventionRuleRepository, never())
                .save(any(ConventionRule.class));
    }

    @Test
    @DisplayName("커밋 규칙을 수정한다")
    void updateConventionRule() {
        ConventionRule rule = new ConventionRule(
                connectedRepository,
                "feat(api):",
                "feat",
                "api",
                "project",
                50,
                false
        );

        ConventionRuleRequest request =
                new ConventionRuleRequest(
                        "feat(backend):",
                        "feat",
                        "backend",
                        "project",
                        40
                );

        mockOwnedRepository();

        when(conventionRuleRepository
                .findByIdAndConnectedRepository(
                        1L,
                        connectedRepository
                ))
                .thenReturn(Optional.of(rule));

        when(conventionRuleRepository
                .existsByConnectedRepositoryAndMessagePatternAndIdNot(
                        connectedRepository,
                        "feat(backend):",
                        1L
                ))
                .thenReturn(false);

        ConventionRuleResponse response =
                repositoryRuleCommandService.updateConventionRule(
                        1L,
                        11L,
                        1L,
                        request
                );

        assertThat(response.messagePattern())
                .isEqualTo("feat(backend):");
        assertThat(response.commitType())
                .isEqualTo("feat");
        assertThat(response.scope())
                .isEqualTo("backend");
        assertThat(response.category())
                .isEqualTo("project");
        assertThat(response.priority())
                .isEqualTo(40);
    }

    @Test
    @DisplayName("커밋 규칙을 비활성화한다")
    void disableConventionRule() {
        ConventionRule rule = new ConventionRule(
                connectedRepository,
                "feat(api):",
                "feat",
                "api",
                "project",
                50,
                false
        );

        mockOwnedRepository();

        when(conventionRuleRepository
                .findByIdAndConnectedRepository(
                        1L,
                        connectedRepository
                ))
                .thenReturn(Optional.of(rule));

        ConventionRuleResponse response =
                repositoryRuleCommandService.updateConventionEnabled(
                        1L,
                        11L,
                        1L,
                        false
                );

        assertThat(response.enabled())
                .isFalse();
    }

    @Test
    @DisplayName("커밋 규칙을 삭제한다")
    void deleteConventionRule() {
        ConventionRule rule = new ConventionRule(
                connectedRepository,
                "feat(api):",
                "feat",
                "api",
                "project",
                50,
                false
        );

        mockOwnedRepository();

        when(conventionRuleRepository
                .findByIdAndConnectedRepository(
                        1L,
                        connectedRepository
                ))
                .thenReturn(Optional.of(rule));

        repositoryRuleCommandService.deleteConventionRule(
                1L,
                11L,
                1L
        );

        verify(conventionRuleRepository)
                .delete(rule);
    }

    @Test
    @DisplayName("다른 사용자의 연결 저장소에는 접근할 수 없다")
    void failWhenConnectedRepositoryIsNotOwnedByUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(connectedRepositoryRepository.findByIdAndUser(
                11L,
                user
        )).thenReturn(Optional.empty());

        ClassificationRuleRequest request =
                new ClassificationRuleRequest(
                        "docs/spring/**",
                        "lectures",
                        "spring",
                        50
                );

        assertThatThrownBy(() ->
                repositoryRuleCommandService.createClassificationRule(
                        1L,
                        11L,
                        request
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("연결된 저장소를 찾을 수 없습니다.");

        verify(classificationRuleRepository, never())
                .save(any(ClassificationRule.class));
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