package com.example.banking.controller;

import com.example.banking.dto.LoanRequest;
import com.example.banking.entity.Loan;
import com.example.banking.security.UserDetailsImpl;
import com.example.banking.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<Loan> applyForLoan(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody LoanRequest loanRequest) {
        Loan loan = loanService.applyForLoan(userDetails.getUser(), loanRequest);
        return new ResponseEntity<>(loan, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Loan>> getMyLoans(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Loan> loans = loanService.getLoansByUser(userDetails.getUser());
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        Loan loan = loanService.getLoanById(id, userDetails.getUser());
        return ResponseEntity.ok(loan);
    }
}
