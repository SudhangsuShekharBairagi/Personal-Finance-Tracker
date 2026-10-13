package com.personalfinancetracker.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personalfinancetracker.entity.RecurringExecution;

public interface RecurringExecutionRepository extends JpaRepository<RecurringExecution, Long> {
    boolean existsByRecurringTransactionIdAndOccurrenceDate(Long recurringTransactionId, LocalDate occurrenceDate);

    long countByRecurringTransactionId(Long recurringTransactionId);
}