package com.personalfinancetracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personalfinancetracker.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
        List<Budget> findAllByUserId(Long userId);

        List<Budget> findByUserIdAndBudgetYearAndBudgetMonthOrderByCategoryAsc(
                        Long userId,
                        Integer budgetYear,
                        Integer budgetMonth);

        Optional<Budget> findByIdAndUserId(Long id, Long userId);

        boolean existsByUserIdAndBudgetYearAndBudgetMonthAndCategoryKey(
                        Long userId,
                        Integer budgetYear,
                        Integer budgetMonth,
                        String categoryKey);

        boolean existsByUserIdAndBudgetYearAndBudgetMonthAndCategoryKeyAndIdNot(
                        Long userId,
                        Integer budgetYear,
                        Integer budgetMonth,
                        String categoryKey,
                        Long id);
}