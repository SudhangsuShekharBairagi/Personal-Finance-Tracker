package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.personalfinancetracker.dto.RecurringTransactionRequest;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.RecurringExecution;
import com.personalfinancetracker.entity.RecurringStatus;
import com.personalfinancetracker.entity.TransactionType;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.RecurringExecutionRepository;
import com.personalfinancetracker.repository.RecurringTransactionRepository;
import com.personalfinancetracker.service.TransactionService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-recurring-failure-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cookie-secure=false",
        "app.recurring.enabled=false"
})
class RecurringFailureIntegrationTest {
    private static final LocalDate TODAY = LocalDate.parse("2026-10-13");

    @Autowired
    private RecurringTransactionService recurringService;

    @Autowired
    private RecurringOccurrenceProcessor occurrenceProcessor;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private RecurringExecutionRepository executionRepository;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void transactionFailureRollsBackExecutionAndDoesNotAdvanceSchedule() {
        AppUser owner = new AppUser("Failure Test", "failure-" + UUID.randomUUID() + "@example.test",
                "unused-password-hash");
        owner.verifyEmail();
        owner = userRepository.saveAndFlush(owner);
        var recurring = recurringService.create(owner.getId(), new RecurringTransactionRequest(
                "Rent", new BigDecimal("500.00"), "expense", "Housing", "DAILY", TODAY, null, null));
        Long recurringId = recurring.id();

        doThrow(new IllegalStateException("simulated transaction persistence failure"))
                .when(transactionService)
                .createRecurringTransaction(
                        any(AppUser.class),
                        anyString(),
                        any(BigDecimal.class),
                        any(TransactionType.class),
                        anyString(),
                        any(LocalDate.class),
                        any(RecurringExecution.class));

        assertThrows(IllegalStateException.class, () -> occurrenceProcessor.processOne(recurringId, TODAY));

        assertEquals(0, executionRepository.countByRecurringTransactionId(recurringId));
        var persisted = recurringRepository.findById(recurringId).orElseThrow();
        assertEquals(TODAY, persisted.getNextScheduledDate());
        assertEquals(RecurringStatus.ACTIVE, persisted.getStatus());
    }
}