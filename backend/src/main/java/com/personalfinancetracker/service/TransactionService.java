package com.personalfinancetracker.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.personalfinancetracker.dto.TransactionRequest;
import com.personalfinancetracker.dto.TransactionResponse;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.RecurringExecution;
import com.personalfinancetracker.entity.Transaction;
import com.personalfinancetracker.entity.TransactionType;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.TransactionRepository;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AppUserRepository appUserRepository;
    private final BudgetAlertService budgetAlertService;

    public TransactionService(
            TransactionRepository transactionRepository,
            AppUserRepository appUserRepository,
            BudgetAlertService budgetAlertService) {
        this.transactionRepository = transactionRepository;
        this.appUserRepository = appUserRepository;
        this.budgetAlertService = budgetAlertService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        return TransactionResponse.from(findOwnedTransaction(userId, transactionId));
    }

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        validateRequest(request);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                        "Sign in to access this resource."));

        TransactionType type = TransactionType.valueOf(normalizeEnum(request.type()));
        Transaction transaction = new Transaction(
                user,
                normalizeText(request.description()),
                normalizeAmount(request.amount()),
                type,
                normalizeText(request.category()),
                request.date());

        Transaction saved = transactionRepository.save(transaction);
        registerBudgetEvaluationAfterCommit(userId);
        return TransactionResponse.from(saved);
    }

    @Transactional
    public Transaction createRecurringTransaction(
            AppUser user,
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            java.time.LocalDate occurrenceDate,
            RecurringExecution execution) {
        Transaction transaction = new Transaction(
                user,
                normalizeText(description),
                normalizeAmount(amount),
                type,
                normalizeText(category),
                occurrenceDate,
                execution);
        Transaction saved = transactionRepository.saveAndFlush(transaction);
        registerBudgetEvaluationAfterCommit(user.getId());
        return saved;
    }

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request) {
        validateRequest(request);
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        TransactionType type = TransactionType.valueOf(normalizeEnum(request.type()));

        transaction.updateDetails(
                normalizeText(request.description()),
                normalizeAmount(request.amount()),
                type,
                normalizeText(request.category()),
                request.date());

        transactionRepository.save(transaction);
        registerBudgetEvaluationAfterCommit(userId);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);
        transactionRepository.delete(transaction);
        registerBudgetEvaluationAfterCommit(userId);
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

    private Transaction findOwnedTransaction(Long userId, Long transactionId) {
        return transactionRepository.findById(transactionId)
                .map(transaction -> {
                    if (!userId.equals(transaction.getUser().getId())) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "TRANSACTION_ACCESS_DENIED",
                                "You do not have access to this transaction.");
                    }
                    return transaction;
                })
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND",
                        "Transaction not found."));
    }

    private void validateRequest(TransactionRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_INVALID", "Transaction details are required.");
        }

        String description = normalizeText(request.description());
        if (description.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_INVALID", "Description is required.");
        }

        String category = normalizeText(request.category());
        if (category.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_INVALID", "Category is required.");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_INVALID", "Amount must be greater than zero.");
        }

        if (request.date() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_INVALID", "Date is required.");
        }

        String type = normalizeEnum(request.type());
        try {
            TransactionType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TRANSACTION_INVALID",
                    "Type must be either income or expense.");
        }
    }

    private String normalizeEnum(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
