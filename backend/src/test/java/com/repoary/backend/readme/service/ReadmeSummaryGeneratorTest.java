package com.repoary.backend.readme.service;

import com.repoary.backend.common.exception.BusinessException;
import com.repoary.backend.readme.exception.ReadmeErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadmeSummaryGeneratorTest {

    private final ReadmeSummaryGenerator generator =
            new ReadmeSummaryGenerator();

    @Test
    void 오늘_학습_정리를_README_Summary로_변환한다() {
        String tilContent = """
                # 2026-09-10 TIL (Today I Learned)

                ## 오늘 학습 정리

                **practice**

                - Kotlin 컬렉션 기반 회원 관리 실습 [🔗 kotlin](https://github.com/example/practice/kotlin)
                - Spring Boot Kotlin 게시판 실습 [🔗 springboot](https://github.com/example/practice/springboot)

                **assignments**

                - 코루틴 동시성 과제 [🔗 coroutinepractice](https://github.com/example/assignments/coroutinepractice)

                ## 오늘 배운 내용

                Kotlin 컬렉션과 코루틴을 학습했다.
                """;

        String summary = generator.generate(tilContent);

        assertThat(summary).isEqualTo(
                "Kotlin 컬렉션 기반 회원 관리 실습 · " +
                        "Spring Boot Kotlin 게시판 실습 / " +
                        "코루틴 동시성 과제"
        );
    }

    @Test
    void GitHub_링크를_README_Summary에서_제거한다() {
        String tilContent = """
                ## 오늘 학습 정리

                **practice**

                - Kotlin 컬렉션 실습 [🔗 kotlin](https://github.com/example)

                ## 오늘 배운 내용
                내용
                """;

        String summary = generator.generate(tilContent);

        assertThat(summary)
                .isEqualTo("Kotlin 컬렉션 실습");
    }

    @Test
    void 같은_카테고리의_항목은_가운데점으로_연결한다() {
        String tilContent = """
                ## 오늘 학습 정리

                **practice**

                - Kotlin 기본 문법 실습
                - Kotlin 컬렉션 실습

                ## 오늘 배운 내용
                내용
                """;

        String summary = generator.generate(tilContent);

        assertThat(summary)
                .isEqualTo(
                        "Kotlin 기본 문법 실습 · Kotlin 컬렉션 실습"
                );
    }

    @Test
    void 다른_카테고리의_항목은_슬래시로_연결한다() {
        String tilContent = """
                ## 오늘 학습 정리

                **lectures**

                - Kotlin 컬렉션 학습

                **practice**

                - 회원 관리 실습

                **assignments**

                - 코루틴 과제

                ## 오늘 배운 내용
                내용
                """;

        String summary = generator.generate(tilContent);

        assertThat(summary)
                .isEqualTo(
                        "Kotlin 컬렉션 학습 / 회원 관리 실습 / 코루틴 과제"
                );
    }

    @Test
    void 오늘_학습_정리_섹션이_없으면_예외가_발생한다() {
        String tilContent = """
                # 2026-09-10 TIL

                ## 오늘 배운 내용

                Kotlin을 학습했다.
                """;

        assertThatThrownBy(
                () -> generator.generate(tilContent)
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ReadmeErrorCode.INVALID_TIL_FORMAT)
                );
    }

    @Test
    void 오늘_학습_정리에_학습_항목이_없으면_예외가_발생한다() {
        String tilContent = """
                ## 오늘 학습 정리

                **practice**

                ## 오늘 배운 내용

                내용
                """;

        assertThatThrownBy(
                () -> generator.generate(tilContent)
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ReadmeErrorCode.INVALID_TIL_FORMAT)
                );
    }

    @Test
    void TIL_내용이_비어있으면_예외가_발생한다() {
        assertThatThrownBy(
                () -> generator.generate(" ")
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ReadmeErrorCode.INVALID_TIL_FORMAT)
                );
    }
}
