package com.personalfinancetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
        @NotBlank(message = "Description is required.")
        @Size(max = 120, message = "Description must be 120 characters or fewer.")
        String description,

        @NotNull(message = "Amount is required.")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
        BigDecimal amount,

        @NotBlank(message = "Type is required.")
        String type,

        @NotBlank(message = "Category is required.")
        @Size(max = 50, message = "Category must be 50 characters or fewer.")
        String category,

        @NotNull(message = "Date is required.")
        LocalDate date) {
}
