package com.personalfinancetracker.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.personalfinancetracker.repository.RecurringTransactionRepository;

@Component
public class RecurringTransactionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final RecurringTransactionRepository recurringRepository;
    private final RecurringOccurrenceProcessor occurrenceProcessor;
    private final int batchSize;

    public RecurringTransactionScheduler(
            RecurringTransactionRepository recurringRepository,
            RecurringOccurrenceProcessor occurrenceProcessor,
            @Value("${app.recurring.batch-size:100}") int batchSize) {
        this.recurringRepository = recurringRepository;
        this.occurrenceProcessor = occurrenceProcessor;
        this.batchSize = Math.max(1, batchSize);
    }

    public int processDueOccurrences(LocalDate today) {
        List<Long> dueIds = recurringRepository.findDueIds(today, PageRequest.of(0, batchSize));
        int processed = 0;
        for (Long recurringId : dueIds) {
            try {
                if (occurrenceProcessor.processOne(recurringId, today)) {
                    processed++;
                }
            } catch (DataIntegrityViolationException exception) {
                logger.info("Skipped a duplicate recurring occurrence for template {}.", recurringId);
            } catch (RuntimeException exception) {
                logger.error("Recurring template {} failed during processing ({}).",
                        recurringId, exception.getClass().getSimpleName());
            }
        }
        return processed;
    }
}