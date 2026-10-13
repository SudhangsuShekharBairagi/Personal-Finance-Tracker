package com.personalfinancetracker.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.personalfinancetracker.dto.BudgetRequest;
import com.personalfinancetracker.dto.BudgetResponse;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.security.AppUserPrincipal;
import com.personalfinancetracker.service.BudgetService;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> listBudgets(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month) {
        return budgetService.listBudgets(
                principal.getId(), parsePeriodPart(year), parsePeriodPart(month));
    }

    @GetMapping("/{id}")
    public BudgetResponse getBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        return budgetService.getBudget(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public BudgetResponse updateBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        return budgetService.updateBudget(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        budgetService.deleteBudget(principal.getId(), id);
    }

    private int parsePeriodPart(String value) {
        try {
            if (value == null || value.isBlank()) {
                throw new NumberFormatException("Missing period value");
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "BUDGET_PERIOD_INVALID", "Year and month must be numeric values.");
        }
    }
}