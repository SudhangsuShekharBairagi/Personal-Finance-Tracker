package com.personalfinancetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.personalfinancetracker.dto.BudgetRequest;
import com.personalfinancetracker.dto.BudgetResponse;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.Budget;
import com.personalfinancetracker.entity.BudgetStatus;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.BudgetNotificationRepository;
import com.personalfinancetracker.repository.BudgetRepository;
import com.personalfinancetracker.repository.TransactionRepository;

@Service
public class BudgetService {
    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);
    private static final BigDecimal EIGHTY = new BigDecimal("80");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final BudgetNotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetAlertService budgetAlertService;

    public BudgetService(
            BudgetRepository budgetRepository,
            BudgetNotificationRepository notificationRepository,
            AppUserRepository appUserRepository,
            TransactionRepository transactionRepository,
            BudgetAlertService budgetAlertService) {
        this.budgetRepository = budgetRepository;
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
        this.transactionRepository = transactionRepository;
        this.budgetAlertService = budgetAlertService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> listBudgets(Long userId, int year, int month) {
        YearMonth period = validatePeriod(year, month);
        return budgetRepository.findByUserIdAndBudgetYearAndBudgetMonthOrderByCategoryAsc(
                userId, year, month)
                .stream()
                .map(budget -> toResponse(userId, budget, period))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(Long userId, Long budgetId) {
        Budget budget = findOwnedBudget(userId, budgetId);
        return toResponse(userId, budget, YearMonth.of(budget.getBudgetYear(), budget.getBudgetMonth()));
    }

    @Transactional
    public BudgetResponse createBudget(Long userId, BudgetRequest request) {
        validateRequest(request);
        YearMonth period = validatePeriod(request.year(), request.month());
        String category = normalizeCategory(request.category());
        String categoryKey = category.toUpperCase(Locale.ROOT);

        if (budgetRepository.existsByUserIdAndBudgetYearAndBudgetMonthAndCategoryKey(
                userId, period.getYear(), period.getMonthValue(), categoryKey)) {
            throw duplicateBudget();
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED", "Sign in to access this resource."));
        Budget budget = new Budget(
                user,
                period.getYear(),
                period.getMonthValue(),
                category,
                categoryKey,
                request.amount());
        try {
            budget = budgetRepository.saveAndFlush(budget);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateBudget();
        }
        registerBudgetEvaluationAfterCommit(userId);
        return toResponse(userId, budget, period);
    }

    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, BudgetRequest request) {
        validateRequest(request);
        YearMonth period = validatePeriod(request.year(), request.month());
        Budget budget = findOwnedBudget(userId, budgetId);
        String category = normalizeCategory(request.category());
        String categoryKey = category.toUpperCase(Locale.ROOT);

        if (budgetRepository.existsByUserIdAndBudgetYearAndBudgetMonthAndCategoryKeyAndIdNot(
                userId, period.getYear(), period.getMonthValue(), categoryKey, budgetId)) {
            throw duplicateBudget();
        }

        budget.updateDetails(
                period.getYear(), period.getMonthValue(), category, categoryKey, request.amount());
        try {
            budget = budgetRepository.saveAndFlush(budget);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateBudget();
        }
        registerBudgetEvaluationAfterCommit(userId);
        return toResponse(userId, budget, period);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = findOwnedBudget(userId, budgetId);
        notificationRepository.detachBudgetNotifications(budget.getId());
        budgetRepository.delete(budget);
    }

    private void registerBudgetEvaluationAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        budgetAlertService.evaluateBudgetsForUser(userId);
                    } catch (RuntimeException exception) {
                        logger.warn("Budget alert evaluation failed for user id {}: {}",
                                userId, exception.getClass().getSimpleName());
                    }
                }
            });
        }
    }

    private BudgetResponse toResponse(Long userId, Budget budget, YearMonth period) {
        LocalDate start = period.atDay(1);
        LocalDate end = period.plusMonths(1).atDay(1);
        BigDecimal spending = transactionRepository.sumExpenseAmountForCategoryAndPeriod(
                userId, normalizeCategory(budget.getCategory()), start, end);
        spending = spending.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal remaining = budget.getAmount().subtract(spending).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal percentage = spending.multiply(ONE_HUNDRED)
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
        BudgetStatus status = statusFor(spending, budget.getAmount());
        return BudgetResponse.from(budget, spending, remaining, percentage, status);
    }

    private BudgetStatus statusFor(BigDecimal spending, BigDecimal limit) {
        int comparedToLimit = spending.compareTo(limit);
        if (comparedToLimit >= 0) {
            return BudgetStatus.EXCEEDED;
        }
        if (spending.multiply(ONE_HUNDRED).compareTo(limit.multiply(EIGHTY)) >= 0) {
            return BudgetStatus.WARNING;
        }
        return BudgetStatus.NORMAL;
    }

    private Budget findOwnedBudget(Long userId, Long budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "BUDGET_NOT_FOUND", "Budget not found."));
    }

    private void validateRequest(BudgetRequest request) {
        if (request == null
                || request.year() == null
                || request.month() == null
                || request.amount() == null
                || request.amount().compareTo(new BigDecimal("0.01")) < 0
                || request.amount().scale() > 2
                || request.amount().precision() - request.amount().scale() > 17
                || request.category() == null
                || normalizeCategory(request.category()).isBlank()
                || normalizeCategory(request.category()).length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BUDGET_INVALID",
                    "Provide a valid period, category, and positive limit with at most two decimal places.");
        }
        validatePeriod(request.year(), request.month());
    }

    private YearMonth validatePeriod(int year, int month) {
        try {
            if (year < 1 || year > 9999) {
                throw new DateTimeException("Year outside supported range");
            }
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BUDGET_PERIOD_INVALID", "Year must be 1-9999 and month must be 1-12.");
        }
    }

    private String normalizeCategory(String category) {
        return category == null ? "" : category.trim();
    }

    private ApiException duplicateBudget() {
        return new ApiException(HttpStatus.CONFLICT,
                "BUDGET_DUPLICATE", "A budget already exists for this category and month.");
    }
}