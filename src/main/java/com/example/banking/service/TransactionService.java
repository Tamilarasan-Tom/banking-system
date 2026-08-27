package com.example.banking.service;

import com.example.banking.dto.TransactionRequest;
import com.example.banking.dto.TransferRequest;
import com.example.banking.entity.Transaction;
import com.example.banking.entity.User;
import java.util.List;

public interface TransactionService {
    Transaction deposit(User user, TransactionRequest request);

    Transaction withdraw(User user, TransactionRequest request);

    void transfer(User user, TransferRequest request);

    void createTransactionPin(User user, String pin);

    List<Transaction> getTransactionHistory(User user);

    List<Transaction> getTransactionHistoryByAccount(Long accountId, User user);

    List<Transaction> getAllTransactions();
}
