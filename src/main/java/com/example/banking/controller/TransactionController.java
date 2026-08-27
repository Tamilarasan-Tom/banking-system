package com.example.banking.controller;

import com.example.banking.dto.TransactionRequest;
import com.example.banking.entity.Transaction;
import com.example.banking.security.UserDetailsImpl;
import com.example.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TransactionRequest request) {
        Transaction txn = transactionService.deposit(userDetails.getUser(), request);
        return ResponseEntity.ok(txn);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TransactionRequest request) {
        Transaction txn = transactionService.withdraw(userDetails.getUser(), request);
        return ResponseEntity.ok(txn);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactionHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Transaction> txns = transactionService.getTransactionHistory(userDetails.getUser());
        return ResponseEntity.ok(txns);
    }
}
