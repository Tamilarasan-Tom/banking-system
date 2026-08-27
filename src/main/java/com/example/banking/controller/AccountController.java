package com.example.banking.controller;

import com.example.banking.dto.AccountRequest;
import com.example.banking.dto.AccountResponse;
import com.example.banking.entity.Account;
import com.example.banking.security.UserDetailsImpl;
import com.example.banking.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AccountRequest accountRequest) {
        Account account = accountService.createAccount(userDetails.getUser(), accountRequest);
        return new ResponseEntity<>(mapToResponse(account), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Account> accounts = accountService.getAccountsByUser(userDetails.getUser());
        List<AccountResponse> responseList = accounts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        Account account = accountService.getAccountById(id, userDetails.getUser());
        return ResponseEntity.ok(mapToResponse(account));
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType().name(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
