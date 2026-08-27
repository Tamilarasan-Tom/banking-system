package com.example.banking.controller;

import com.example.banking.dto.TransferRequest;
import com.example.banking.entity.Transaction;
import com.example.banking.security.UserDetailsImpl;
import com.example.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransactionService transactionService;

    public TransferController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> transfer(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TransferRequest request) {
        transactionService.transfer(userDetails.getUser(), request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Funds transferred successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransfersHistory(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Transaction> allTxns = transactionService.getTransactionHistory(userDetails.getUser());
        List<Transaction> transfers = allTxns.stream()
                .filter(txn -> txn.getTransactionType() == com.example.banking.entity.TransactionType.TRANSFER)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transfers);
    }
}
