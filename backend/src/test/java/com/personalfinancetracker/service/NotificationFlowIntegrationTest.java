package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.personalfinancetracker.dto.BudgetRequest;
import com.personalfinancetracker.dto.TransactionRequest;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.Budget;
import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;
import com.personalfinancetracker.entity.NotificationType;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.BudgetNotificationRepository;
import com.personalfinancetracker.repository.BudgetRepository;
import com.personalfinancetracker.security.AppUserPrincipal;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-notification-flow-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cookie-secure=false",
        "app.recurring.enabled=false",
        "app.notifications.fixed-delay-ms=3600000",
        "app.notifications.max-attempts=5",
        "app.notifications.batch-size=50"
})
@AutoConfigureMockMvc
class NotificationFlowIntegrationTest {
    private static final int YEAR = 2026;
    private static final int MONTH = 10;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetAlertService budgetAlertService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetNotificationRepository notificationRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailService mailService;

    private AppUser owner;
    private AppUser anotherUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        String suffix = UUID.randomUUID().toString();
        owner = userRepository.saveAndFlush(verifiedUser("owner-" + suffix + "@example.test"));
        anotherUser = userRepository.saveAndFlush(verifiedUser("other-" + suffix + "@example.test"));
    }

    @Test
    void committedExpensesPersistWarningAndExceededNotifications() {
        createBudget(owner, "Groceries", "100.00");

        createExpense(owner, "Groceries", "80.00");
        assertEquals(NotificationStatus.PENDING, notification(owner, "Groceries", "WARNING", "100.00").getStatus());

        createExpense(owner, "Groceries", "20.00");
        assertEquals(NotificationStatus.PENDING, notification(owner, "Groceries", "EXCEEDED", "100.00").getStatus());
    }

    @Test
    void jumpingPastLimitCreatesExceededAndSupersedesWarning() {
        createBudget(owner, "Groceries", "100.00");
        createExpense(owner, "Groceries", "50.00");
        createExpense(owner, "Groceries", "51.00");

        assertEquals(NotificationStatus.SKIPPED,
                notification(owner, "Groceries", "WARNING", "100.00").getStatus());
        assertEquals("Superseded by exceeded alert",
                notification(owner, "Groceries", "WARNING", "100.00").getFailureReason());
        assertEquals(NotificationStatus.PENDING,
                notification(owner, "Groceries", "EXCEEDED", "100.00").getStatus());
    }

    @Test
    void creatingBudgetEvaluatesAlreadyRecordedSpending() {
        createExpense(owner, "Groceries", "110.00");

        createBudget(owner, "Groceries", "100.00");

        assertEquals(NotificationStatus.PENDING,
                notification(owner, "Groceries", "EXCEEDED", "100.00").getStatus());
        assertEquals(NotificationStatus.SKIPPED,
                notification(owner, "Groceries", "WARNING", "100.00").getStatus());
    }

    @Test
    void changedLimitAllowsFreshAlertButRepeatingStateDoesNotDuplicate() {
        Budget budget = createBudget(owner, "Groceries", "100.00");
        createExpense(owner, "Groceries", "80.00");

        budgetService.updateBudget(owner.getId(), budget.getId(), budgetRequest("Groceries", "90.00"));
        budgetService.updateBudget(owner.getId(), budget.getId(), budgetRequest("Groceries", "90.00"));

        assertEquals(2, notificationRepository.findVisibleByUserIdAndOptionalFilters(
                owner.getId(), null, NotificationType.WARNING, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(NotificationStatus.PENDING,
                notification(owner, "Groceries", "WARNING", "90.00").getStatus());
    }

    @Test
    void deletingBudgetDetachesNotificationsAndPreservesHistory() {
        Budget budget = createBudget(owner, "Groceries", "100.00");
        createExpense(owner, "Groceries", "80.00");
        Long notificationId = notification(owner, "Groceries", "WARNING", "100.00").getId();

        budgetService.deleteBudget(owner.getId(), budget.getId());

        BudgetNotification savedNotification = notificationRepository.findById(notificationId).orElseThrow();
        assertNull(savedNotification.getBudget());
        assertEquals("Groceries", savedNotification.getCategory());
        assertEquals(MONTH, savedNotification.getBudgetMonth());
        assertEquals(YEAR, savedNotification.getBudgetYear());
    }

    @Test
    void retriesFailedMailWithBackoffAndSkipsNotificationsThatAreNotDue() {
        createBudget(owner, "Groceries", "100.00");
        createExpense(owner, "Groceries", "80.00");
        BudgetNotification notification = notification(owner, "Groceries", "WARNING", "100.00");
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(mailService)
                .sendBudgetAlert(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        budgetAlertService.processPendingNotifications();

        notification = notificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        assertTrue(notification.getNextAttemptAt().isAfter(Instant.now()));
        assertEquals(1, notification.getAttemptCount());
        budgetAlertService.processPendingNotifications();
        assertEquals(1, notificationRepository.findById(notification.getId()).orElseThrow().getAttemptCount());
        verify(mailService, times(1)).sendBudgetAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        for (int attempt = 2; attempt <= 5; attempt++) {
            notification = notificationRepository.findById(notification.getId()).orElseThrow();
            notification.markRetrying("Delivery failed.", Instant.EPOCH);
            notificationRepository.saveAndFlush(notification);
            budgetAlertService.processPendingNotifications();
        }

        notification = notificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(5, notification.getAttemptCount());
        assertNull(notification.getNextAttemptAt());
        verify(mailService, times(5)).sendBudgetAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void listingCombinesFiltersExcludesSkippedAndScopesToCurrentUser() throws Exception {
        createBudget(owner, "Groceries", "100.00");
        createExpense(owner, "Groceries", "110.00");
        createBudget(owner, "Dining", "100.00");
        createExpense(owner, "Dining", "80.00");

        createBudget(anotherUser, "Groceries", "100.00");
        createExpense(anotherUser, "Groceries", "110.00");

        mockMvc.perform(get("/api/notifications")
                .with(user(new AppUserPrincipal(owner)))
                .param("status", "PENDING")
                .param("type", "EXCEEDED")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].category").value("Groceries"))
                .andExpect(jsonPath("$.content[0].type").value("EXCEEDED"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        mockMvc.perform(get("/api/notifications")
                .with(user(new AppUserPrincipal(owner)))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private Budget createBudget(AppUser user, String category, String limit) {
        budgetService.createBudget(user.getId(), budgetRequest(category, limit));
        return budgetRepository.findAllByUserId(user.getId()).stream()
                .filter(budget -> budget.getCategory().equals(category))
                .findFirst()
                .orElseThrow();
    }

    private void createExpense(AppUser user, String category, String amount) {
        transactionService.createTransaction(user.getId(),
                new TransactionRequest("Test expense", new BigDecimal(amount), "expense", category,
                        LocalDate.of(YEAR, MONTH, 15)));
    }

    private BudgetNotification notification(AppUser user, String category, String type, String limit) {
        String idempotencyKey = "budget:" + user.getId() + ":" + YEAR + ":" + MONTH + ":"
                + category.toLowerCase(Locale.ROOT) + ":" + type + ":" + limit;
        return notificationRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey).orElseThrow();
    }

    private BudgetRequest budgetRequest(String category, String limit) {
        return new BudgetRequest(YEAR, MONTH, category, new BigDecimal(limit));
    }

    private AppUser verifiedUser(String email) {
        AppUser user = new AppUser("Notification Test", email, "unused-password-hash");
        user.verifyEmail();
        return user;
    }
}
