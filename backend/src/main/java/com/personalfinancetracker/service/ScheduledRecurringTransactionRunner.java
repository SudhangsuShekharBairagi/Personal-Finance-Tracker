package com.personalfinancetracker.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.recurring.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledRecurringTransactionRunner {
    private final RecurringTransactionScheduler recurringScheduler;
    private final Clock clock;

    public ScheduledRecurringTransactionRunner(RecurringTransactionScheduler recurringScheduler, Clock clock) {
        this.recurringScheduler = recurringScheduler;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.recurring.fixed-delay-ms:60000}")
    public void runScheduled() {
        recurringScheduler.processDueOccurrences(LocalDate.now(clock));
    }
}