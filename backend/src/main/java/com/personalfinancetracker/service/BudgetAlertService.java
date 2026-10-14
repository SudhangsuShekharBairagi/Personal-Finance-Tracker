package com.personalfinancetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    private final BudgetRepository budgetRepository;
    private final AppUserRepository appUserRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetNotificationRepository notificationRepository;
    private final BudgetNotificationDeliveryService deliveryService;
    private final int notificationBatchSize;

    public BudgetAlertService(
            BudgetRepository budgetRepository,
            AppUserRepository appUserRepository,
            TransactionRepository transactionRepository,
            BudgetNotificationRepository notificationRepository,
            BudgetNotificationDeliveryService deliveryService,
            @Value("${app.notifications.batch-size:50}") int notificationBatchSize) {
        this.budgetRepository = budgetRepository;
        this.appUserRepository = appUserRepository;
        this.transactionRepository = transactionRepository;
        this.notificationRepository = notificationRepository;
        this.deliveryService = deliveryService;
        this.notificationBatchSize = notificationBatchSize;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    public void processPendingNotifications() {
        List<Long> dueIds = notificationRepository.findDueNotificationIds(
                List.of(NotificationStatus.PENDING, NotificationStatus.RETRYING),
                Instant.now(),
                PageRequest.of(0, notificationBatchSize));
        for (Long notificationId : dueIds) {
            try {
                deliveryService.processNotification(notificationId);
            } catch (Exception exception) {
                logger.warn("Budget alert processing failed for notification id {}: {}",
                        notificationId, exception.getClass().getSimpleName());
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

        boolean exceeded = hasReachedThreshold(spending, limit, ONE_HUNDRED);
        if (exceeded) {
            String warningKey = buildIdempotencyKey(budget, NotificationType.WARNING, limit);
            if (notificationRepository.findByUserIdAndIdempotencyKey(user.getId(), warningKey).isEmpty()) {
                BudgetNotification warning = createNotification(
                        user, budget, NotificationType.WARNING, EIGHTY, warningKey, spending, limit);
                warning.markSkipped("Superseded by exceeded alert");
                notificationRepository.save(warning);
            }
            createNotificationIfAbsent(user, budget, NotificationType.EXCEEDED, ONE_HUNDRED, spending, limit);
        } else if (hasReachedThreshold(spending, limit, EIGHTY)) {
            createNotificationIfAbsent(user, budget, NotificationType.WARNING, EIGHTY, spending, limit);
        }
    }

    private boolean hasReachedThreshold(BigDecimal spending, BigDecimal limit, BigDecimal threshold) {
        BigDecimal thresholdAmount = limit.multiply(threshold).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        return spending.compareTo(thresholdAmount) >= 0;
    }

    private void createNotificationIfAbsent(
            AppUser user,
            Budget budget,
            NotificationType type,
            BigDecimal threshold,
            BigDecimal spending,
            BigDecimal limit) {
        String key = buildIdempotencyKey(budget, type, limit);
        if (notificationRepository.findByUserIdAndIdempotencyKey(user.getId(), key).isEmpty()) {
            notificationRepository.save(createNotification(user, budget, type, threshold, key, spending, limit));
        }
    }

    private BudgetNotification createNotification(
            AppUser user,
            Budget budget,
            NotificationType type,
            BigDecimal threshold,
            String key,
            BigDecimal spending,
            BigDecimal limit) {
        return new BudgetNotification(
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
    }

    private String buildIdempotencyKey(Budget budget, NotificationType type, BigDecimal limit) {
        return "budget:" + budget.getUser().getId() + ":" + budget.getBudgetYear() + ":"
                + budget.getBudgetMonth() + ":" + budget.getCategoryKey().toLowerCase(Locale.ROOT)
                + ":" + type.name() + ":" + limit.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
