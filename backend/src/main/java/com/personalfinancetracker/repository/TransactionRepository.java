package com.personalfinancetracker.repository;

import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.personalfinancetracker.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select coalesce(sum(transaction.amount), 0)
            from Transaction transaction
            where transaction.user.id = :userId
                and transaction.type = com.personalfinancetracker.entity.TransactionType.EXPENSE
                and lower(trim(transaction.category)) = lower(:category)
                and transaction.date >= :periodStart
                and transaction.date < :periodEnd
            """)
    BigDecimal sumExpenseAmountForCategoryAndPeriod(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);
}
