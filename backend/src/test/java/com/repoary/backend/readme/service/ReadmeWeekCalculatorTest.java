package com.repoary.backend.readme.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReadmeWeekCalculatorTest {

    private final ReadmeWeekCalculator calculator =
            new ReadmeWeekCalculator();

    @Test
    void 월초가_월요일이_아니면_첫째_주는_월초부터_일요일까지다() {
        var result =
                calculator.calculate(
                        LocalDate.of(2026, 9, 1)
                );

        assertThat(result.weekNumber()).isEqualTo(1);
        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 9, 6));
    }

    @Test
    void 둘째_주는_월요일부터_일요일까지다() {
        var result =
                calculator.calculate(
                        LocalDate.of(2026, 9, 10)
                );

        assertThat(result.weekNumber()).isEqualTo(2);
        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 9, 13));
    }

    @Test
    void 월말을_넘는_주는_월말에서_끝난다() {
        var result =
                calculator.calculate(
                        LocalDate.of(2026, 9, 30)
                );

        assertThat(result.weekNumber()).isEqualTo(5);
        assertThat(result.startDate())
                .isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(result.endDate())
                .isEqualTo(LocalDate.of(2026, 9, 30));
    }
}