package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.personalfinancetracker.dto.RecurringTransactionRequest;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.RecurringExecutionStatus;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.RecurringExecutionRepository;
import com.personalfinancetracker.repository.RecurringTransactionRepository;
import com.personalfinancetracker.repository.TransactionRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-recurring-concurrency-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cookie-secure=false",
        "app.recurring.enabled=false"
})
class RecurringConcurrencyIntegrationTest {
    private static final LocalDate TODAY = LocalDate.parse("2026-10-13");

    @Autowired
    private RecurringOccurrenceProcessor occurrenceProcessor;

    @Autowired
    private RecurringTransactionService recurringService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private RecurringExecutionRepository executionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void concurrentWorkersPersistOneTransactionForTheDueOccurrence() throws Exception {
        String email = "worker-" + UUID.randomUUID() + "@example.test";
        AppUser owner = new AppUser("Worker Test", email, "unused-password-hash");
        owner.verifyEmail();
        owner = userRepository.saveAndFlush(owner);
        var recurring = recurringService.create(owner.getId(), new RecurringTransactionRequest(
                "Monthly rent",
                new BigDecimal("750.00"),
                "expense",
                "Housing",
                "MONTHLY",
                TODAY,
                null,
                null));

        Long recurringId = recurring.id();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = workers.submit(() -> processTogether(recurringId, ready, start));
            Future<Boolean> second = workers.submit(() -> processTogether(recurringId, ready, start));
            ready.await();
            start.countDown();

            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
            assertEquals(1, executionRepository.countByRecurringTransactionId(recurringId));
            assertEquals(RecurringExecutionStatus.COMPLETED, executionRepository.findAll().getFirst().getStatus());
            assertEquals(1, transactionRepository.findByUserId(owner.getId(), PageRequest.of(0, 10))
                    .getTotalElements());
            assertEquals(TODAY.plusMonths(1), recurringRepository.findById(recurringId).orElseThrow()
                    .getNextScheduledDate());
        } finally {
            workers.shutdownNow();
        }
    }

    private boolean processTogether(Long recurringId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return occurrenceProcessor.processOne(recurringId, TODAY);
    }
}