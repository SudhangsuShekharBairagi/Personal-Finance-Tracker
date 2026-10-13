package com.personalfinancetracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.personalfinancetracker.entity.Budget;
import com.personalfinancetracker.entity.BudgetStatus;

public record BudgetResponse(
        Long id,
        Integer year,
        Integer month,
        String category,
        String budgetLimit,
        String actualSpending,
        String remainingAmount,
        String percentageUsed,
        BudgetStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static BudgetResponse from(
            Budget budget,
            BigDecimal actualSpending,
            BigDecimal remainingAmount,
            BigDecimal percentageUsed,
            BudgetStatus status) {
        return new BudgetResponse(
                budget.getId(),
                budget.getBudgetYear(),
                budget.getBudgetMonth(),
                budget.getCategory(),
                budget.getAmount().toPlainString(),
                actualSpending.toPlainString(),
                remainingAmount.toPlainString(),
                percentageUsed.toPlainString(),
                status,
                budget.getCreatedAt(),
                budget.getUpdatedAt());
    }
}