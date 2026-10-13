package com.personalfinancetracker.service;

import java.time.LocalDate;
import java.time.YearMonth;

import com.personalfinancetracker.entity.RecurrenceFrequency;

public final class RecurrenceDates {
    private RecurrenceDates() {
    }

    public static LocalDate nextOccurrence(
            LocalDate currentOccurrence,
            RecurrenceFrequency frequency,
            int anchorDayOfMonth,
            int anchorMonth) {
        return switch (frequency) {
            case DAILY -> currentOccurrence.plusDays(1);
            case WEEKLY -> currentOccurrence.plusWeeks(1);
            case MONTHLY -> {
                YearMonth nextMonth = YearMonth.from(currentOccurrence).plusMonths(1);
                yield nextMonth.atDay(Math.min(anchorDayOfMonth, nextMonth.lengthOfMonth()));
            }
            case YEARLY -> {
                YearMonth nextYear = YearMonth.of(currentOccurrence.getYear() + 1, anchorMonth);
                yield nextYear.atDay(Math.min(anchorDayOfMonth, nextYear.lengthOfMonth()));
            }
        };
    }
}