package com.personalfinancetracker.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import org.hibernate.annotations.Check;

@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(name = "uk_budgets_owner_period_category", columnNames = {
        "user_id", "budget_year", "budget_month", "category_key" }), indexes = {
                @Index(name = "idx_budgets_owner_period", columnList = "user_id, budget_year, budget_month")
        })
@Check(constraints = "budget_amount > 0 AND budget_month BETWEEN 1 AND 12 AND budget_year BETWEEN 1 AND 9999")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;

    @Column(name = "budget_month", nullable = false)
    private Integer budgetMonth;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "category_key", nullable = false, length = 50)
    private String categoryKey;

    @Column(name = "budget_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Budget() {
    }

    public Budget(
            AppUser user,
            Integer budgetYear,
            Integer budgetMonth,
            String category,
            String categoryKey,
            BigDecimal amount) {
        this.user = user;
        this.budgetYear = budgetYear;
        this.budgetMonth = budgetMonth;
        this.category = category;
        this.categoryKey = categoryKey;
        this.amount = amount;
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

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public Integer getBudgetMonth() {
        return budgetMonth;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            Integer budgetYear,
            Integer budgetMonth,
            String category,
            String categoryKey,
            BigDecimal amount) {
        this.budgetYear = budgetYear;
        this.budgetMonth = budgetMonth;
        this.category = category;
        this.categoryKey = categoryKey;
        this.amount = amount;
    }
}