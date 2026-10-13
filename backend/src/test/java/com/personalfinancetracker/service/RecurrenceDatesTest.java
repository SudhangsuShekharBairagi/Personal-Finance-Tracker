package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.personalfinancetracker.entity.RecurrenceFrequency;

class RecurrenceDatesTest {
    @Test
    void advancesDailyAndWeeklyUsingCalendarDays() {
        assertEquals(LocalDate.parse("2024-03-01"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2024-02-29"), RecurrenceFrequency.DAILY, 29, 2));
        assertEquals(LocalDate.parse("2024-03-07"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2024-02-29"), RecurrenceFrequency.WEEKLY, 29, 2));
    }

    @Test
    void monthlyDatesClampAndThenRestoreTheAnchorDay() {
        assertEquals(LocalDate.parse("2025-02-28"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2025-01-31"), RecurrenceFrequency.MONTHLY, 31, 1));
        assertEquals(LocalDate.parse("2025-03-31"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2025-02-28"), RecurrenceFrequency.MONTHLY, 31, 1));
        assertEquals(LocalDate.parse("2024-02-29"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2024-01-31"), RecurrenceFrequency.MONTHLY, 31, 1));
        assertEquals(LocalDate.parse("2025-03-30"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2025-02-28"), RecurrenceFrequency.MONTHLY, 30, 1));
        assertEquals(LocalDate.parse("2025-03-29"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2025-02-28"), RecurrenceFrequency.MONTHLY, 29, 2));
    }

    @Test
    void yearlyFebruaryTwentyNinthReturnsInLeapYears() {
        assertEquals(LocalDate.parse("2025-02-28"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2024-02-29"), RecurrenceFrequency.YEARLY, 29, 2));
        assertEquals(LocalDate.parse("2028-02-29"), RecurrenceDates.nextOccurrence(
                LocalDate.parse("2027-02-28"), RecurrenceFrequency.YEARLY, 29, 2));
    }
}