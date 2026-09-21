package com.repoary.backend.readme.service;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.readme.dto.ReadmeRowResponse;
import com.repoary.backend.readme.exception.ReadmeErrorCode;
import com.repoary.backend.repository.exception.RepositoryErrorCode;
import com.repoary.backend.til.exception.TilErrorCode;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.til.domain.TilDocument;
import com.repoary.backend.til.repository.TilDocumentRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReadmeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConnectedRepositoryRepository connectedRepositoryRepository;

    @Mock
    private TilDocumentRepository tilDocumentRepository;

    @Mock
    private ReadmeSummaryGenerator readmeSummaryGenerator;

    @Mock
    private ReadmeWeekCalculator readmeWeekCalculator;

    @InjectMocks
    private ReadmeService readmeService;

    private User user;
    private ConnectedRepository connectedRepository;
    private TilDocument tilDocument;

    @BeforeEach
    void setUp() {
        user = org.mockito.Mockito.mock(User.class);
        connectedRepository =
                org.mockito.Mockito.mock(ConnectedRepository.class);
        tilDocument =
                org.mockito.Mockito.mock(TilDocument.class);
    }

    @Test
    void TIL이_존재하면_README_행을_생성한다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        LocalDate targetDate =
                LocalDate.of(2026, 9, 10);

        String tilContent = """
                ## 오늘 학습 정리

                **practice**
                - Kotlin 컬렉션 기반 회원 관리 실습
                """;

        String summary =
                "Kotlin 컬렉션 기반 회원 관리 실습";

        ReadmeWeekCalculator.WeekInfo weekInfo =
                new ReadmeWeekCalculator.WeekInfo(
                        2,
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2026, 9, 13)
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(
                connectedRepositoryRepository.findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
        ).willReturn(Optional.of(connectedRepository));

        given(
                tilDocumentRepository
                        .findByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
        ).willReturn(Optional.of(tilDocument));

        given(tilDocument.getContent())
                .willReturn(tilContent);

        given(readmeSummaryGenerator.generate(tilContent))
                .willReturn(summary);

        given(readmeWeekCalculator.calculate(targetDate))
                .willReturn(weekInfo);

        ReadmeRowResponse response =
                readmeService.generateRow(
                        userId,
                        connectedRepositoryId,
                        targetDate
                );

        assertThat(response.targetDate())
                .isEqualTo(targetDate);

        assertThat(response.weekNumber())
                .isEqualTo(2);

        assertThat(response.weekStartDate())
                .isEqualTo(
                        LocalDate.of(2026, 9, 7)
                );

        assertThat(response.weekEndDate())
                .isEqualTo(
                        LocalDate.of(2026, 9, 13)
                );

        assertThat(response.weekLabel())
                .isEqualTo(
                        "Week 2 (2026-09-07 ~ 2026-09-13)"
                );

        assertThat(response.summary())
                .isEqualTo(summary);

        assertThat(response.markdown())
                .isEqualTo(
                        "| [2026-09-10](./2026-09-10.md) | " +
                                "Kotlin 컬렉션 기반 회원 관리 실습 |"
                );
    }

    @Test
    void TIL이_없으면_README_행을_생성할_수_없다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        LocalDate targetDate =
                LocalDate.of(2026, 9, 10);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(
                connectedRepositoryRepository.findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
        ).willReturn(Optional.of(connectedRepository));

        given(
                tilDocumentRepository
                        .findByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
        ).willReturn(Optional.empty());

        assertThatThrownBy(
                () -> readmeService.generateRow(
                        userId,
                        connectedRepositoryId,
                        targetDate
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(TilErrorCode.DATE_NOT_FOUND)
                );
    }

    @Test
    void 다른_사용자의_저장소에서는_README_행을_생성할_수_없다() {
        Long userId = 1L;
        Long connectedRepositoryId = 10L;
        LocalDate targetDate =
                LocalDate.of(2026, 9, 10);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(
                connectedRepositoryRepository.findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
        ).willReturn(Optional.empty());

        assertThatThrownBy(
                () -> readmeService.generateRow(
                        userId,
                        connectedRepositoryId,
                        targetDate
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(RepositoryErrorCode.CONNECTED_REPOSITORY_NOT_FOUND)
                );
    }

    @Test
    void README_날짜가_없으면_예외가_발생한다() {
        assertThatThrownBy(
                () -> readmeService.generateRow(
                        1L,
                        10L,
                        null
                )
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ReadmeErrorCode.DATE_REQUIRED)
                );
    }
}
