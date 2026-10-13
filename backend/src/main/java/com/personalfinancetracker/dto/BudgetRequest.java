package com.personalfinancetracker.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BudgetRequest(
        @NotNull(message = "Year is required.") @Min(value = 1, message = "Year must be between 1 and 9999.") @Max(value = 9999, message = "Year must be between 1 and 9999.") Integer year,

        @NotNull(message = "Month is required.") @Min(value = 1, message = "Month must be between 1 and 12.") @Max(value = 12, message = "Month must be between 1 and 12.") Integer month,

        @NotBlank(message = "Category is required.") @Size(max = 50, message = "Category must be 50 characters or fewer.") String category,

        @NotNull(message = "Budget limit is required.") @DecimalMin(value = "0.01", message = "Budget limit must be greater than zero.") @Digits(integer = 17, fraction = 2, message = "Budget limit must not exceed 17 whole digits or 2 decimal places.") BigDecimal amount) {
}