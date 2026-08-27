package com.example.banking.service;

import com.example.banking.dto.LoanRequest;
import com.example.banking.entity.Loan;
import com.example.banking.entity.User;
import java.util.List;

public interface LoanService {
    Loan applyForLoan(User user, LoanRequest request);
    Loan getLoanById(Long id, User user);
    List<Loan> getLoansByUser(User user);
    List<Loan> getLoansByUserId(Long userId);
    List<Loan> getAllLoans();
    void approveLoan(Long id, String adminRemarks);
    void rejectLoan(Long id, String adminRemarks);
}
