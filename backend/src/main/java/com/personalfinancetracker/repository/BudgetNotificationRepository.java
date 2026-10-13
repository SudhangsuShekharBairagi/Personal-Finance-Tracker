package com.personalfinancetracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;

import jakarta.persistence.LockModeType;

public interface BudgetNotificationRepository extends JpaRepository<BudgetNotification, Long> {
    Optional<BudgetNotification> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Page<BudgetNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<BudgetNotification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status,
            Pageable pageable);

    Page<BudgetNotification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(Long userId,
            com.personalfinancetracker.entity.NotificationType notificationType, Pageable pageable);

    Optional<BudgetNotification> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from BudgetNotification n where n.status in :statuses order by n.createdAt asc")
    List<BudgetNotification> findPendingForProcessing(@Param("statuses") List<NotificationStatus> statuses);
}
