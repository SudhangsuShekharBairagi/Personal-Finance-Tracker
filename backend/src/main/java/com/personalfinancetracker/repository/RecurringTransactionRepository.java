package com.personalfinancetracker.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.personalfinancetracker.entity.RecurringStatus;
import com.personalfinancetracker.entity.RecurringTransaction;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUserIdAndStatusNotOrderByNextScheduledDateAsc(
            Long userId,
            RecurringStatus status);

    Optional<RecurringTransaction> findByIdAndUserIdAndStatusNot(
            Long id,
            Long userId,
            RecurringStatus status);

    @Query("""
            select recurring.id
            from RecurringTransaction recurring
            where recurring.status = com.personalfinancetracker.entity.RecurringStatus.ACTIVE
              and (recurring.nextScheduledDate <= :today
                   or (recurring.endDate is not null and recurring.endDate < :today))
            order by recurring.nextScheduledDate asc, recurring.id asc
            """)
    List<Long> findDueIds(@Param("today") LocalDate today, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select recurring from RecurringTransaction recurring where recurring.id = :id")
    Optional<RecurringTransaction> findByIdForUpdate(@Param("id") Long id);
}