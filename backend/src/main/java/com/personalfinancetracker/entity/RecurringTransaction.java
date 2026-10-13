package com.personalfinancetracker.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

import org.hibernate.annotations.Check;

@Entity
@Table(name = "recurring_transactions", indexes = {
        @Index(name = "idx_recurring_due", columnList = "status, next_scheduled_date, id"),
        @Index(name = "idx_recurring_owner_status", columnList = "user_id, status")
})
@Check(constraints = "amount > 0 AND anchor_day_of_month BETWEEN 1 AND 31 AND anchor_month BETWEEN 1 AND 12 AND (end_date IS NULL OR end_date >= start_date)")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_scheduled_date", nullable = false)
    private LocalDate nextScheduledDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "anchor_day_of_month", nullable = false)
    private Integer anchorDayOfMonth;

    @Column(name = "anchor_month", nullable = false)
    private Integer anchorMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringStatus status;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(
            AppUser user,
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        this.user = user;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
        this.nextScheduledDate = startDate;
        this.endDate = endDate;
        this.anchorDayOfMonth = startDate.getDayOfMonth();
        this.anchorMonth = startDate.getMonthValue();
        this.status = RecurringStatus.ACTIVE;
        this.notes = notes;
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

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getNextScheduledDate() {
        return nextScheduledDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getAnchorDayOfMonth() {
        return anchorDayOfMonth;
    }

    public Integer getAnchorMonth() {
        return anchorMonth;
    }

    public RecurringStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.anchorDayOfMonth = startDate.getDayOfMonth();
        this.anchorMonth = startDate.getMonthValue();
        if (startDate.isAfter(nextScheduledDate)) {
            nextScheduledDate = startDate;
        }
        this.notes = notes;
        if (status == RecurringStatus.ACTIVE && endDate != null && nextScheduledDate.isAfter(endDate)) {
            status = RecurringStatus.ENDED;
        }
    }

    public void advanceTo(LocalDate nextDate) {
        nextScheduledDate = nextDate;
        if (endDate != null && nextDate.isAfter(endDate) && status == RecurringStatus.ACTIVE) {
            status = RecurringStatus.ENDED;
        }
    }

    public void pause() {
        if (status == RecurringStatus.ACTIVE) {
            status = RecurringStatus.PAUSED;
        }
    }

    public void resume() {
        if (status == RecurringStatus.PAUSED) {
            status = RecurringStatus.ACTIVE;
        }
    }

    public void end() {
        if (status == RecurringStatus.ACTIVE) {
            status = RecurringStatus.ENDED;
        }
    }

    public void softDelete() {
        status = RecurringStatus.DELETED;
    }
}