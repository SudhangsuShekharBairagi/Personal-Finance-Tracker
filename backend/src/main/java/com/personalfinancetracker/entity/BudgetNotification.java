package com.personalfinancetracker.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "budget_notifications", uniqueConstraints = @UniqueConstraint(name = "uk_budget_notifications_user_key", columnNames = {
        "user_id", "idempotency_key" }), indexes = {
                @Index(name = "idx_budget_notifications_user_status", columnList = "user_id, status"),
                @Index(name = "idx_budget_notifications_created_at", columnList = "created_at")
        })
public class BudgetNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private Budget budget;

    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;

    @Column(name = "budget_month", nullable = false)
    private Integer budgetMonth;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType notificationType;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "actual_spending", nullable = false, precision = 19, scale = 2)
    private BigDecimal actualSpending;

    @Column(name = "budget_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "threshold_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal thresholdPercent;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BudgetNotification() {
    }

    public BudgetNotification(
            AppUser user,
            Budget budget,
            Integer budgetYear,
            Integer budgetMonth,
            String category,
            NotificationType notificationType,
            String recipientEmail,
            String idempotencyKey,
            BigDecimal actualSpending,
            BigDecimal budgetLimit,
            BigDecimal thresholdPercent) {
        this.user = user;
        this.budget = budget;
        this.budgetYear = budgetYear;
        this.budgetMonth = budgetMonth;
        this.category = category;
        this.notificationType = notificationType;
        this.recipientEmail = recipientEmail;
        this.idempotencyKey = idempotencyKey;
        this.actualSpending = actualSpending;
        this.budgetLimit = budgetLimit;
        this.thresholdPercent = thresholdPercent;
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Budget getBudget() {
        return budget;
    }

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public Integer getBudgetMonth() {
        return budgetMonth;
    }

    public String getCategory() {
        return category;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getActualSpending() {
        return actualSpending;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markRetrying(String failureReason) {
        this.status = NotificationStatus.RETRYING;
        this.failureReason = failureReason;
        this.lastAttemptAt = Instant.now();
        this.attemptCount++;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.lastAttemptAt = sentAt;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = failureReason;
        this.lastAttemptAt = Instant.now();
        this.attemptCount++;
    }

    public void markSkipped(String failureReason) {
        this.status = NotificationStatus.SKIPPED;
        this.failureReason = failureReason;
        this.lastAttemptAt = Instant.now();
    }
}
