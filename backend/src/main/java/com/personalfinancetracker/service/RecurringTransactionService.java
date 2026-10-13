package com.personalfinancetracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.dto.RecurringTransactionRequest;
import com.personalfinancetracker.dto.RecurringTransactionResponse;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.RecurrenceFrequency;
import com.personalfinancetracker.entity.RecurringStatus;
import com.personalfinancetracker.entity.RecurringTransaction;
import com.personalfinancetracker.entity.TransactionType;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.RecurringTransactionRepository;

@Service
public class RecurringTransactionService {
    private final RecurringTransactionRepository recurringRepository;
    private final AppUserRepository appUserRepository;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringRepository,
            AppUserRepository appUserRepository) {
        this.recurringRepository = recurringRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> list(Long userId) {
        return recurringRepository
                .findByUserIdAndStatusNotOrderByNextScheduledDateAsc(userId, RecurringStatus.DELETED)
                .stream()
                .map(RecurringTransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse get(Long userId, Long recurringId) {
        return RecurringTransactionResponse.from(findOwned(userId, recurringId));
    }

    @Transactional
    public RecurringTransactionResponse create(Long userId, RecurringTransactionRequest request) {
        ValidatedRecurring validated = validate(request);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED", "Sign in to access this resource."));
        RecurringTransaction recurring = new RecurringTransaction(
                user,
                validated.description(),
                validated.amount(),
                validated.type(),
                validated.category(),
                validated.frequency(),
                validated.startDate(),
                validated.endDate(),
                validated.notes());
        return RecurringTransactionResponse.from(recurringRepository.save(recurring));
    }

    @Transactional
    public RecurringTransactionResponse update(
            Long userId,
            Long recurringId,
            RecurringTransactionRequest request) {
        ValidatedRecurring validated = validate(request);
        RecurringTransaction recurring = findOwnedForUpdate(userId, recurringId);
        recurring.updateDetails(
                validated.description(),
                validated.amount(),
                validated.type(),
                validated.category(),
                validated.frequency(),
                validated.startDate(),
                validated.endDate(),
                validated.notes());
        return RecurringTransactionResponse.from(recurringRepository.save(recurring));
    }

    @Transactional
    public RecurringTransactionResponse pause(Long userId, Long recurringId) {
        RecurringTransaction recurring = findOwnedForUpdate(userId, recurringId);
        recurring.pause();
        return RecurringTransactionResponse.from(recurringRepository.save(recurring));
    }

    @Transactional
    public RecurringTransactionResponse resume(Long userId, Long recurringId) {
        RecurringTransaction recurring = findOwnedForUpdate(userId, recurringId);
        if (recurring.getStatus() == RecurringStatus.ENDED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "RECURRING_TRANSACTION_ENDED", "Create a new template to start this schedule again.");
        }
        if (recurring.getStatus() == RecurringStatus.PAUSED
                && recurring.getEndDate() != null
                && recurring.getNextScheduledDate().isAfter(recurring.getEndDate())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "RECURRING_END_DATE_PASSED", "Edit the end date before resuming this schedule.");
        }
        recurring.resume();
        return RecurringTransactionResponse.from(recurringRepository.save(recurring));
    }

    @Transactional
    public void delete(Long userId, Long recurringId) {
        RecurringTransaction recurring = findOwnedForUpdate(userId, recurringId);
        recurring.softDelete();
        recurringRepository.save(recurring);
    }

    private RecurringTransaction findOwned(Long userId, Long recurringId) {
        return recurringRepository.findByIdAndUserIdAndStatusNot(
                recurringId, userId, RecurringStatus.DELETED)
                .orElseThrow(() -> notFound());
    }

    private RecurringTransaction findOwnedForUpdate(Long userId, Long recurringId) {
        RecurringTransaction recurring = recurringRepository.findByIdForUpdate(recurringId)
                .orElseThrow(() -> notFound());
        if (!userId.equals(recurring.getUser().getId())
                || recurring.getStatus() == RecurringStatus.DELETED) {
            throw notFound();
        }
        return recurring;
    }

    private ValidatedRecurring validate(RecurringTransactionRequest request) {
        if (request == null || request.amount() == null || request.startDate() == null
                || request.amount().compareTo(new BigDecimal("0.01")) < 0
                || request.amount().scale() > 2
                || request.amount().precision() - request.amount().scale() > 17) {
            throw invalidRequest();
        }
        String description = clean(request.description());
        String category = clean(request.category());
        String notes = clean(request.notes());
        if (description.isBlank() || description.length() > 120
                || category.isBlank() || category.length() > 50
                || notes.length() > 500
                || !supportedDate(request.startDate())
                || request.endDate() != null && (!supportedDate(request.endDate())
                        || request.endDate().isBefore(request.startDate()))) {
            throw invalidRequest();
        }

        try {
            TransactionType type = TransactionType.valueOf(upper(request.type()));
            RecurrenceFrequency frequency = RecurrenceFrequency.valueOf(upper(request.frequency()));
            BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
            return new ValidatedRecurring(
                    description,
                    amount,
                    type,
                    category,
                    frequency,
                    request.startDate(),
                    request.endDate(),
                    notes.isBlank() ? null : notes);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw invalidRequest();
        }
    }

    private boolean supportedDate(LocalDate date) {
        return date.getYear() >= 1000 && date.getYear() <= 9999;
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private ApiException invalidRequest() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "RECURRING_TRANSACTION_INVALID",
                "Provide a valid description, amount, type, category, frequency, and date range.");
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND,
                "RECURRING_TRANSACTION_NOT_FOUND", "Recurring transaction not found.");
    }

    private record ValidatedRecurring(
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
    }
}