package com.personalfinancetracker.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.personalfinancetracker.dto.TransactionRequest;
import com.personalfinancetracker.dto.TransactionResponse;
import com.personalfinancetracker.security.AppUserPrincipal;
import com.personalfinancetracker.service.TransactionService;

@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/transactions")
    public Page<TransactionResponse> listTransactions(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 25, sort = "date", direction = Direction.DESC) Pageable pageable) {
        return transactionService.listTransactions(principal.getId(), pageable);
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse getTransaction(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        return transactionService.getTransaction(principal.getId(), id);
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(principal.getId(), request));
    }

    @PutMapping("/transactions/{id}")
    public TransactionResponse updateTransaction(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        return transactionService.updateTransaction(principal.getId(), id, request);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        transactionService.deleteTransaction(principal.getId(), id);
    }
}
