package com.example.banking.controller;

import com.example.banking.dto.*;
import com.example.banking.entity.*;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.LoanRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import com.example.banking.service.LoanService;
import com.example.banking.service.TransactionService;
import com.example.banking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final LoanService loanService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    public AdminController(UserService userService, AccountService accountService, TransactionService transactionService,
                           LoanService loanService, UserRepository userRepository, AccountRepository accountRepository,
                           TransactionRepository transactionRepository, LoanRepository loanRepository) {
        this.userService = userService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.loanService = loanService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Map<String, String>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest statusRequest) {
        userService.updateUserStatus(id, UserStatus.valueOf(statusRequest.getStatus().toUpperCase()));
        Map<String, String> response = new HashMap<>();
        response.put("message", "User status updated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PutMapping("/accounts/{id}/status")
    public ResponseEntity<Map<String, String>> updateAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountStatusRequest statusRequest) {
        accountService.updateAccountStatus(id, AccountStatus.valueOf(statusRequest.getStatus().toUpperCase()));
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account status updated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/loans")
    public ResponseEntity<List<Loan>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @PutMapping("/loans/{id}/approve")
    public ResponseEntity<Map<String, String>> approveLoan(
            @PathVariable Long id,
            @RequestBody(required = false) LoanApprovalRequest approvalRequest) {
        String remarks = approvalRequest != null ? approvalRequest.getAdminRemarks() : "Approved by administrator";
        loanService.approveLoan(id, remarks);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Loan application approved successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/loans/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectLoan(
            @PathVariable Long id,
            @RequestBody(required = false) LoanApprovalRequest approvalRequest) {
        String remarks = approvalRequest != null ? approvalRequest.getAdminRemarks() : "Rejected by administrator";
        loanService.rejectLoan(id, remarks);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Loan application rejected successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getSystemStats() {
        long totalUsers = userRepository.count();
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long totalTransactions = transactionRepository.count();
        BigDecimal totalTransferAmount = transactionRepository.sumTotalTransfers();
        long pendingLoans = loanRepository.countByLoanStatus(LoanStatus.PENDING);
        long approvedLoans = loanRepository.countByLoanStatus(LoanStatus.APPROVED);
        long rejectedLoans = loanRepository.countByLoanStatus(LoanStatus.REJECTED);

        AdminStatsResponse stats = new AdminStatsResponse(
                totalUsers,
                totalAccounts,
                activeAccounts,
                totalTransactions,
                totalTransferAmount,
                pendingLoans,
                approvedLoans,
                rejectedLoans
        );

        return ResponseEntity.ok(stats);
    }
}
