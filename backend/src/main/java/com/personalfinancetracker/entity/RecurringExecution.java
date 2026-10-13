package com.personalfinancetracker.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recurring_executions", uniqueConstraints = @UniqueConstraint(
        name = "uk_recurring_execution_occurrence",
        columnNames = { "recurring_transaction_id", "occurrence_date" }), indexes = {
                @Index(name = "idx_recurring_execution_date", columnList = "occurrence_date")
        })
public class RecurringExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurring_transaction_id", nullable = false)
    private RecurringTransaction recurringTransaction;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringExecutionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToOne(mappedBy = "recurringExecution", fetch = FetchType.LAZY)
    private Transaction generatedTransaction;

    protected RecurringExecution() {
    }

    public RecurringExecution(RecurringTransaction recurringTransaction, LocalDate occurrenceDate) {
        this.recurringTransaction = recurringTransaction;
        this.occurrenceDate = occurrenceDate;
        this.status = RecurringExecutionStatus.PROCESSING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public RecurringTransaction getRecurringTransaction() {
        return recurringTransaction;
    }

    public LocalDate getOccurrenceDate() {
        return occurrenceDate;
    }

    public RecurringExecutionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Transaction getGeneratedTransaction() {
        return generatedTransaction;
    }

    public void complete() {
        status = RecurringExecutionStatus.COMPLETED;
        completedAt = Instant.now();
    }
}