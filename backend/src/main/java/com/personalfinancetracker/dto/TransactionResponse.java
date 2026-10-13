package com.personalfinancetracker.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import com.personalfinancetracker.entity.Transaction;

public record TransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        String type,
        String category,
        LocalDate date,
        Instant createdAt,
        Instant updatedAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getType().name().toLowerCase(Locale.ROOT),
                transaction.getCategory(),
                transaction.getDate(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
