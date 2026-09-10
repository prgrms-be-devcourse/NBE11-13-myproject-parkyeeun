package com.repoary.backend.readme.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Component
public class ReadmeWeekCalculator {

    public WeekInfo calculate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException(
                    "README 날짜는 필수입니다."
            );
        }

        LocalDate monthStart =
                targetDate.withDayOfMonth(1);

        LocalDate monthEnd =
                targetDate.with(
                        TemporalAdjusters.lastDayOfMonth()
                );

        LocalDate weekStart =
                targetDate.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.MONDAY
                        )
                );

        LocalDate weekEnd =
                targetDate.with(
                        TemporalAdjusters.nextOrSame(
                                DayOfWeek.SUNDAY
                        )
                );

        if (weekStart.isBefore(monthStart)) {
            weekStart = monthStart;
        }

        if (weekEnd.isAfter(monthEnd)) {
            weekEnd = monthEnd;
        }

        int weekNumber =
                calculateWeekNumber(
                        monthStart,
                        targetDate
                );

        return new WeekInfo(
                weekNumber,
                weekStart,
                weekEnd
        );
    }

    private int calculateWeekNumber(
            LocalDate monthStart,
            LocalDate targetDate
    ) {
        LocalDate firstWeekEnd =
                monthStart.with(
                        TemporalAdjusters.nextOrSame(
                                DayOfWeek.SUNDAY
                        )
                );

        if (!targetDate.isAfter(firstWeekEnd)) {
            return 1;
        }

        long daysAfterFirstWeek =
                ChronoUnit.DAYS.between(
                        firstWeekEnd.plusDays(1),
                        targetDate
                );

        return 2 + (int) (daysAfterFirstWeek / 7);
    }

    public record WeekInfo(
            int weekNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}