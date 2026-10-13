package com.personalfinancetracker.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.entity.RecurringExecution;
import com.personalfinancetracker.entity.RecurringStatus;
import com.personalfinancetracker.entity.RecurringTransaction;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.RecurringExecutionRepository;
import com.personalfinancetracker.repository.RecurringTransactionRepository;

import com.personalfinancetracker.service.TransactionService;

@Service
public class RecurringOccurrenceProcessor {
    private final RecurringTransactionRepository recurringRepository;
    private final RecurringExecutionRepository executionRepository;
    private final TransactionService transactionService;

    public RecurringOccurrenceProcessor(
            RecurringTransactionRepository recurringRepository,
            RecurringExecutionRepository executionRepository,
            TransactionService transactionService) {
        this.recurringRepository = recurringRepository;
        this.executionRepository = executionRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public boolean processOne(Long recurringId, LocalDate today) {
        RecurringTransaction recurring = recurringRepository.findByIdForUpdate(recurringId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "RECURRING_TRANSACTION_NOT_FOUND", "Recurring transaction not found."));
        if (recurring.getStatus() != RecurringStatus.ACTIVE) {
            return false;
        }

        LocalDate occurrenceDate = recurring.getNextScheduledDate();
        if (recurring.getEndDate() != null && occurrenceDate.isAfter(recurring.getEndDate())) {
            recurring.end();
            return false;
        }
        if (occurrenceDate.isAfter(today)) {
            return false;
        }

        if (executionRepository.existsByRecurringTransactionIdAndOccurrenceDate(recurringId, occurrenceDate)) {
            recurring.advanceTo(RecurrenceDates.nextOccurrence(
                    occurrenceDate,
                    recurring.getFrequency(),
                    recurring.getAnchorDayOfMonth(),
                    recurring.getAnchorMonth()));
            return false;
        }

        RecurringExecution execution = executionRepository.saveAndFlush(
                new RecurringExecution(recurring, occurrenceDate));
        transactionService.createRecurringTransaction(
                recurring.getUser(),
                recurring.getDescription(),
                recurring.getAmount(),
                recurring.getType(),
                recurring.getCategory(),
                occurrenceDate,
                execution);
        execution.complete();
        executionRepository.save(execution);
        recurring.advanceTo(RecurrenceDates.nextOccurrence(
                occurrenceDate,
                recurring.getFrequency(),
                recurring.getAnchorDayOfMonth(),
                recurring.getAnchorMonth()));
        return true;
    }
}