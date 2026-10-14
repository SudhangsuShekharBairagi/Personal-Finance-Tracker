package com.personalfinancetracker.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.dto.BudgetNotificationResponse;
import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;
import com.personalfinancetracker.entity.NotificationType;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.BudgetNotificationRepository;

@Service
public class NotificationService {
    private final BudgetNotificationRepository notificationRepository;

    public NotificationService(BudgetNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<BudgetNotificationResponse> listNotifications(Long userId, NotificationStatus status,
            NotificationType type, Pageable pageable) {
        return notificationRepository.findVisibleByUserIdAndOptionalFilters(userId, status, type, pageable)
                .map(BudgetNotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public BudgetNotificationResponse getNotification(Long userId, Long notificationId) {
        BudgetNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND", "Notification not found."));
        return BudgetNotificationResponse.from(notification);
    }
}
