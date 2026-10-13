package com.personalfinancetracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;
import com.personalfinancetracker.entity.NotificationType;

public record BudgetNotificationResponse(
        Long id,
        Long budgetId,
        Integer year,
        Integer month,
        String category,
        NotificationType type,
        String status,
        String recipientEmail,
        String actualSpending,
        String budgetLimit,
        String thresholdPercent,
        Instant createdAt,
        Instant updatedAt,
        String failureReason) {

    public static BudgetNotificationResponse from(BudgetNotification notification) {
        return new BudgetNotificationResponse(
                notification.getId(),
                notification.getBudget() == null ? null : notification.getBudget().getId(),
                notification.getBudgetYear(),
                notification.getBudgetMonth(),
                notification.getCategory(),
                notification.getNotificationType(),
                notification.getStatus().name(),
                notification.getRecipientEmail(),
                notification.getActualSpending().toPlainString(),
                notification.getBudgetLimit().toPlainString(),
                notification.getThresholdPercent().toPlainString(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getFailureReason());
    }
}
