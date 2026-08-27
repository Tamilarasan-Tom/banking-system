package com.example.banking.service;

import com.example.banking.dto.AccountRequest;
import com.example.banking.entity.Account;
import com.example.banking.entity.AccountStatus;
import com.example.banking.entity.User;
import java.util.List;

public interface AccountService {
    Account createAccount(User user, AccountRequest request);
    Account getAccountById(Long id, User user);
    Account getAccountByNumber(String accountNumber);
    List<Account> getAccountsByUser(User user);
    List<Account> getAccountsByUserId(Long userId);
    List<Account> getAllAccounts();
    void updateAccountStatus(Long id, AccountStatus status);
}
