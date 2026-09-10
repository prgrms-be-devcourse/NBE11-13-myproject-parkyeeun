package com.repoary.backend.readme.service;

import com.repoary.backend.common.exception.NotFoundException;
import com.repoary.backend.readme.dto.ReadmeRowResponse;
import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.til.domain.TilDocument;
import com.repoary.backend.til.repository.TilDocumentRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ReadmeService {

    private final UserRepository userRepository;
    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final TilDocumentRepository tilDocumentRepository;
    private final ReadmeSummaryGenerator readmeSummaryGenerator;
    private final ReadmeWeekCalculator readmeWeekCalculator;

    public ReadmeService(
            UserRepository userRepository,
            ConnectedRepositoryRepository connectedRepositoryRepository,
            TilDocumentRepository tilDocumentRepository,
            ReadmeSummaryGenerator readmeSummaryGenerator,
            ReadmeWeekCalculator readmeWeekCalculator
    ) {
        this.userRepository = userRepository;
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.tilDocumentRepository = tilDocumentRepository;
        this.readmeSummaryGenerator = readmeSummaryGenerator;
        this.readmeWeekCalculator = readmeWeekCalculator;
    }

    @Transactional(readOnly = true)
    public ReadmeRowResponse generateRow(
            Long userId,
            Long connectedRepositoryId,
            LocalDate targetDate
    ) {
        if (targetDate == null) {
            throw new IllegalArgumentException(
                    "README 날짜는 필수입니다."
            );
        }

        ConnectedRepository connectedRepository =
                getOwnedConnectedRepository(
                        userId,
                        connectedRepositoryId
                );

        TilDocument tilDocument =
                tilDocumentRepository
                        .findByConnectedRepositoryAndTargetDate(
                                connectedRepository,
                                targetDate
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "해당 날짜의 TIL을 찾을 수 없습니다."
                                )
                        );

        String summary =
                readmeSummaryGenerator.generate(
                        tilDocument.getContent()
                );

        ReadmeWeekCalculator.WeekInfo weekInfo =
                readmeWeekCalculator.calculate(
                        targetDate
                );

        String weekLabel = String.format(
                "Week %d (%s ~ %s)",
                weekInfo.weekNumber(),
                weekInfo.startDate(),
                weekInfo.endDate()
        );

        String markdown = String.format(
                "| [%s](./%s.md) | %s |",
                targetDate,
                targetDate,
                summary
        );

        return new ReadmeRowResponse(
                targetDate,
                weekInfo.weekNumber(),
                weekInfo.startDate(),
                weekInfo.endDate(),
                weekLabel,
                summary,
                markdown
        );
    }

    private ConnectedRepository getOwnedConnectedRepository(
            Long userId,
            Long connectedRepositoryId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return connectedRepositoryRepository
                .findByIdAndUser(
                        connectedRepositoryId,
                        user
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "연결된 저장소를 찾을 수 없습니다."
                        )
                );
    }
}