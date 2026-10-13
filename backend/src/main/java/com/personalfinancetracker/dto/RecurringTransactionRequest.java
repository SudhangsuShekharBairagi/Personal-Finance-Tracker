package com.personalfinancetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecurringTransactionRequest(
        @NotBlank(message = "Description is required.") @Size(max = 120, message = "Description must be 120 characters or fewer.") String description,

        @NotNull(message = "Amount is required.") @DecimalMin(value = "0.01", message = "Amount must be greater than zero.") @Digits(integer = 17, fraction = 2, message = "Amount must not exceed 17 whole digits or 2 decimal places.") BigDecimal amount,

        @NotBlank(message = "Type is required.") String type,

        @NotBlank(message = "Category is required.") @Size(max = 50, message = "Category must be 50 characters or fewer.") String category,

        @NotBlank(message = "Frequency is required.") String frequency,

        @NotNull(message = "Start date is required.") LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Notes must be 500 characters or fewer.") String notes) {
}