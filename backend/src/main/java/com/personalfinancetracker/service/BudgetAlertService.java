package com.personalfinancetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.Budget;
import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;
import com.personalfinancetracker.entity.NotificationType;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.BudgetNotificationRepository;
import com.personalfinancetracker.repository.BudgetRepository;
import com.personalfinancetracker.repository.TransactionRepository;

@Service
public class BudgetAlertService {
    private static final Logger logger = LoggerFactory.getLogger(BudgetAlertService.class);
    private static final BigDecimal EIGHTY = new BigDecimal("80");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int MAX_RETRIES = 3;

    private final BudgetRepository budgetRepository;
    private final AppUserRepository appUserRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetNotificationRepository notificationRepository;
    private final MailService mailService;

    public BudgetAlertService(
            BudgetRepository budgetRepository,
            AppUserRepository appUserRepository,
            TransactionRepository transactionRepository,
            BudgetNotificationRepository notificationRepository,
            MailService mailService) {
        this.budgetRepository = budgetRepository;
        this.appUserRepository = appUserRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.mailService = mailService;
    }

    @Transactional
    public void evaluateBudgetsForUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED", "Sign in to access this resource."));
        if (!user.isEmailVerified()) {
            return;
        }

        List<Budget> budgets = budgetRepository.findAllByUserId(userId);
        for (Budget budget : budgets) {
            evaluateBudget(user, budget);
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.fixed-delay-ms:60000}")
    @Transactional
    public void processPendingNotifications() {
        List<BudgetNotification> pending = notificationRepository.findPendingForProcessing(
                List.of(NotificationStatus.PENDING, NotificationStatus.RETRYING));
        for (BudgetNotification notification : pending) {
            if (notification.getAttemptCount() >= MAX_RETRIES) {
                notification.markFailed("Retry limit reached.");
                continue;
            }
            try {
                mailService.sendBudgetAlert(
                        notification.getRecipientEmail(),
                        notification.getCategory(),
                        String.format(Locale.ROOT, "%d-%02d", notification.getBudgetYear(),
                                notification.getBudgetMonth()),
                        notification.getThresholdPercent().toPlainString(),
                        notification.getActualSpending().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        notification.getBudgetLimit().setScale(2, RoundingMode.HALF_UP).toPlainString());
                notification.markSent();
            } catch (Exception exception) {
                logger.warn("Budget alert send failed for notification id {}: {}",
                        notification.getId(), exception.getClass().getSimpleName());
                notification.markRetrying("Delivery failed.");
            }
        }
    }

    private void evaluateBudget(AppUser user, Budget budget) {
        YearMonth period = YearMonth.of(budget.getBudgetYear(), budget.getBudgetMonth());
        LocalDate start = period.atDay(1);
        LocalDate end = period.plusMonths(1).atDay(1);
        BigDecimal spending = transactionRepository.sumExpenseAmountForCategoryAndPeriod(
                user.getId(), budget.getCategoryKey(), start, end);
        spending = spending == null ? BigDecimal.ZERO : spending.setScale(2, RoundingMode.HALF_UP);
        BigDecimal limit = budget.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        for (NotificationType type : List.of(NotificationType.WARNING, NotificationType.EXCEEDED)) {
            BigDecimal threshold = type == NotificationType.WARNING ? EIGHTY : ONE_HUNDRED;
            BigDecimal thresholdAmount = limit.multiply(threshold).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            if (spending.compareTo(thresholdAmount) < 0) {
                continue;
            }

            String key = buildIdempotencyKey(budget, type);
            Optional<BudgetNotification> existing = notificationRepository.findByUserIdAndIdempotencyKey(user.getId(),
                    key);
            if (existing.isPresent()) {
                continue;
            }

            BudgetNotification notification = new BudgetNotification(
                    user,
                    budget,
                    budget.getBudgetYear(),
                    budget.getBudgetMonth(),
                    budget.getCategory(),
                    type,
                    user.getEmail(),
                    key,
                    spending,
                    limit,
                    threshold);
            notificationRepository.save(notification);
        }
    }

    private String buildIdempotencyKey(Budget budget, NotificationType type) {
        return "budget:" + budget.getUser().getId() + ":" + budget.getBudgetYear() + ":"
                + budget.getBudgetMonth() + ":" + budget.getCategoryKey().toLowerCase(Locale.ROOT)
                + ":" + type.name();
    }
}
