package com.repoary.backend.til.service;

import com.repoary.backend.analysis.domain.AnalysisJob;
import com.repoary.backend.analysis.domain.AnalysisJobStatus;
import com.repoary.backend.analysis.dto.StoredAnalysisResult;
import com.repoary.backend.analysis.repository.AnalysisJobRepository;
import com.repoary.backend.common.exception.ConflictException;
import com.repoary.backend.common.exception.NotFoundException;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.til.domain.TilDocument;
import com.repoary.backend.til.repository.TilDocumentRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TilServiceTest {

    private TilDocumentRepository tilDocumentRepository;
    private AnalysisJobRepository analysisJobRepository;
    private UserRepository userRepository;
    private ConnectedRepositoryRepository connectedRepositoryRepository;
    private TilMarkdownGenerator tilMarkdownGenerator;
    private JsonMapper jsonMapper;

    private TilService tilService;

    private User user;
    private ConnectedRepository connectedRepository;

    @BeforeEach
    void setUp() {
        tilDocumentRepository = mock(TilDocumentRepository.class);
        analysisJobRepository = mock(AnalysisJobRepository.class);
        userRepository = mock(UserRepository.class);
        connectedRepositoryRepository =
                mock(ConnectedRepositoryRepository.class);
        tilMarkdownGenerator = mock(TilMarkdownGenerator.class);
        jsonMapper = mock(JsonMapper.class);

        tilService = new TilService(
                tilDocumentRepository,
                analysisJobRepository,
                userRepository,
                connectedRepositoryRepository,
                tilMarkdownGenerator,
                jsonMapper
        );

        user = mock(User.class);
        connectedRepository = mock(ConnectedRepository.class);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(
                connectedRepositoryRepository.findByIdAndUser(
                        11L,
                        user
                )
        ).thenReturn(Optional.of(connectedRepository));
    }

    @Test
    @DisplayName("완료된 분석 결과를 이용해 TIL 초안을 생성한다")
    void createDraft() {
        LocalDate targetDate =
                LocalDate.of(2026, 8, 28);

        AnalysisJob analysisJob = mock(AnalysisJob.class);

        StoredAnalysisResult analysisResult =
                new StoredAnalysisResult(
                        targetDate,
                        1,
                        List.of()
                );

        String resultJson = """
                {
                  "targetDate": "2026-08-28",
                  "commitCount": 1,
                  "commits": []
                }
                """;

        String markdown = """
                # 2026-08-28 TIL (Today I Learned)

                ## 오늘 학습 정리
                """;

        when(
                tilDocumentRepository
                        .existsByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
        ).thenReturn(false);

        when(
                analysisJobRepository
                        .findFirstByConnectedRepositoryAndTargetDateAndStatusOrderByCreatedAtDesc(
                                connectedRepository,
                                targetDate,
                                AnalysisJobStatus.COMPLETED
                        )
        ).thenReturn(Optional.of(analysisJob));

        when(analysisJob.getResult())
                .thenReturn(resultJson);

        when(
                jsonMapper.readValue(
                        resultJson,
                        StoredAnalysisResult.class
                )
        ).thenReturn(analysisResult);

        when(
                tilMarkdownGenerator.generate(
                        analysisResult,
                        connectedRepository
                )
        ).thenReturn(markdown);

        when(
                tilDocumentRepository.save(
                        any(TilDocument.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        TilDocument result = tilService.createDraft(
                1L,
                11L,
                targetDate
        );

        assertThat(result.getTargetDate())
                .isEqualTo(targetDate);

        assertThat(result.getTitle())
                .isEqualTo(
                        "2026-08-28 TIL (Today I Learned)"
                );

        assertThat(result.getContent())
                .isEqualTo(markdown);

        assertThat(result.getAnalysisJob())
                .isEqualTo(analysisJob);

        verify(tilMarkdownGenerator).generate(
                analysisResult,
                connectedRepository
        );

        verify(tilDocumentRepository).save(
                any(TilDocument.class)
        );
    }

    @Test
    @DisplayName("같은 저장소와 날짜에 TIL이 있으면 중복 생성하지 않는다")
    void rejectDuplicateDraft() {
        LocalDate targetDate =
                LocalDate.of(2026, 8, 28);

        when(
                tilDocumentRepository
                        .existsByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                tilService.createDraft(
                        1L,
                        11L,
                        targetDate
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "해당 날짜의 TIL이 이미 존재합니다."
                );

        verifyNoInteractions(
                analysisJobRepository,
                tilMarkdownGenerator
        );

        verify(tilDocumentRepository, never())
                .save(any(TilDocument.class));
    }

    @Test
    @DisplayName("완료된 분석 결과가 없으면 TIL을 생성할 수 없다")
    void rejectDraftWithoutCompletedAnalysis() {
        LocalDate targetDate =
                LocalDate.of(2026, 8, 28);

        when(
                tilDocumentRepository
                        .existsByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
        ).thenReturn(false);

        when(
                analysisJobRepository
                        .findFirstByConnectedRepositoryAndTargetDateAndStatusOrderByCreatedAtDesc(
                                connectedRepository,
                                targetDate,
                                AnalysisJobStatus.COMPLETED
                        )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tilService.createDraft(
                        1L,
                        11L,
                        targetDate
                )
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage(
                        "완료된 분석 결과를 찾을 수 없습니다."
                );

        verify(tilDocumentRepository, never())
                .save(any(TilDocument.class));
    }

    @Test
    @DisplayName("날짜별 TIL을 조회한다")
    void getByDate() {
        LocalDate targetDate =
                LocalDate.of(2026, 8, 28);

        TilDocument tilDocument =
                mock(TilDocument.class);

        when(
                tilDocumentRepository
                        .findByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
        ).thenReturn(Optional.of(tilDocument));

        TilDocument result =
                tilService.getByDate(
                        1L,
                        11L,
                        targetDate
                );

        assertThat(result)
                .isEqualTo(tilDocument);
    }

    @Test
    @DisplayName("TIL 문서 ID로 상세 조회한다")
    void getDocument() {
        TilDocument tilDocument =
                mock(TilDocument.class);

        when(
                tilDocumentRepository
                        .findByIdAndConnectedRepository(
                                31L,
                                connectedRepository
                        )
        ).thenReturn(Optional.of(tilDocument));

        TilDocument result =
                tilService.getDocument(
                        1L,
                        11L,
                        31L
                );

        assertThat(result)
                .isEqualTo(tilDocument);
    }

    @Test
    @DisplayName("TIL Markdown 내용을 수정한다")
    void updateContent() {
        TilDocument tilDocument =
                mock(TilDocument.class);

        String updatedContent = """
                # 2026-08-28 TIL (Today I Learned)

                수정된 TIL 내용
                """;

        when(
                tilDocumentRepository
                        .findByIdAndConnectedRepository(
                                31L,
                                connectedRepository
                        )
        ).thenReturn(Optional.of(tilDocument));

        TilDocument result =
                tilService.updateContent(
                        1L,
                        11L,
                        31L,
                        updatedContent
                );

        assertThat(result)
                .isEqualTo(tilDocument);

        verify(tilDocument)
                .updateContent(updatedContent);
    }

    @Test
    @DisplayName("사용자 소유가 아닌 저장소에는 접근할 수 없다")
    void rejectUnownedRepository() {
        LocalDate targetDate =
                LocalDate.of(2026, 8, 28);

        when(
                connectedRepositoryRepository.findByIdAndUser(
                        11L,
                        user
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tilService.createDraft(
                        1L,
                        11L,
                        targetDate
                )
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage(
                        "연결된 저장소를 찾을 수 없습니다."
                );

        verifyNoInteractions(
                analysisJobRepository,
                tilDocumentRepository,
                tilMarkdownGenerator
        );
    }

    @Test
    @DisplayName("다른 사용자 저장소의 TIL 상세 문서에 접근할 수 없다")
    void rejectUnownedTilDocument() {
        when(
                connectedRepositoryRepository.findByIdAndUser(
                        11L,
                        user
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tilService.getDocument(
                        1L,
                        11L,
                        31L
                )
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage(
                        "연결된 저장소를 찾을 수 없습니다."
                );

        verify(
                tilDocumentRepository,
                never()
        ).findByIdAndConnectedRepository(
                any(),
                any()
        );
    }
}