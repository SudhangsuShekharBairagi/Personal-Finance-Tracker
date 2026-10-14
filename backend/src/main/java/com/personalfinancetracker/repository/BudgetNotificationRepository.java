package com.personalfinancetracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;

import jakarta.persistence.LockModeType;

public interface BudgetNotificationRepository extends JpaRepository<BudgetNotification, Long> {
    Optional<BudgetNotification> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
            select n from BudgetNotification n
            where n.user.id = :userId
              and n.status <> com.personalfinancetracker.entity.NotificationStatus.SKIPPED
              and (:status is null or n.status = :status)
              and (:type is null or n.notificationType = :type)
            order by n.createdAt desc
            """)
    Page<BudgetNotification> findVisibleByUserIdAndOptionalFilters(
            @Param("userId") Long userId,
            @Param("status") NotificationStatus status,
            @Param("type") com.personalfinancetracker.entity.NotificationType type,
            Pageable pageable);

    Optional<BudgetNotification> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from BudgetNotification n where n.id = :id")
    Optional<BudgetNotification> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select n.id from BudgetNotification n
            where n.status in :statuses
              and (n.nextAttemptAt is null or n.nextAttemptAt <= :now)
            order by n.createdAt asc
            """)
    List<Long> findDueNotificationIds(
            @Param("statuses") List<NotificationStatus> statuses,
            @Param("now") java.time.Instant now,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update BudgetNotification n set n.budget = null where n.budget.id = :budgetId")
    int detachBudgetNotifications(@Param("budgetId") Long budgetId);
}
