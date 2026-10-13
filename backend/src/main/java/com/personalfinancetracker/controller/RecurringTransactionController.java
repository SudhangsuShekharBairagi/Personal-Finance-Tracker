package com.personalfinancetracker.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.personalfinancetracker.dto.RecurringTransactionRequest;
import com.personalfinancetracker.dto.RecurringTransactionResponse;
import com.personalfinancetracker.security.AppUserPrincipal;
import com.personalfinancetracker.service.RecurringTransactionService;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {
    private final RecurringTransactionService recurringService;

    public RecurringTransactionController(RecurringTransactionService recurringService) {
        this.recurringService = recurringService;
    }

    @GetMapping
    public List<RecurringTransactionResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return recurringService.list(principal.getId());
    }

    @GetMapping("/{id}")
    public RecurringTransactionResponse get(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        return recurringService.get(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody RecurringTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request) {
        return recurringService.update(principal.getId(), id, request);
    }

    @PatchMapping("/{id}/pause")
    public RecurringTransactionResponse pause(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        return recurringService.pause(principal.getId(), id);
    }

    @PatchMapping("/{id}/resume")
    public RecurringTransactionResponse resume(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        return recurringService.resume(principal.getId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        recurringService.delete(principal.getId(), id);
    }
}