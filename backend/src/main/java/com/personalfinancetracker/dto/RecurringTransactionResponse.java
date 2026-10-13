package com.personalfinancetracker.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import com.personalfinancetracker.entity.RecurringStatus;
import com.personalfinancetracker.entity.RecurringTransaction;

public record RecurringTransactionResponse(
        Long id,
        String description,
        String amount,
        String type,
        String category,
        String frequency,
        LocalDate startDate,
        LocalDate nextScheduledDate,
        LocalDate endDate,
        RecurringStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static RecurringTransactionResponse from(RecurringTransaction transaction) {
        return new RecurringTransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount().toPlainString(),
                transaction.getType().name().toLowerCase(Locale.ROOT),
                transaction.getCategory(),
                transaction.getFrequency().name(),
                transaction.getStartDate(),
                transaction.getNextScheduledDate(),
                transaction.getEndDate(),
                transaction.getStatus(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}