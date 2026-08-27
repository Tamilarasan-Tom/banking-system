package com.example.banking.service;

import com.example.banking.dto.LoanRequest;
import com.example.banking.entity.*;
import com.example.banking.exception.InvalidTransactionException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.exception.UnauthorizedException;
import com.example.banking.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final NotificationService notificationService;

    public LoanServiceImpl(LoanRepository loanRepository, NotificationService notificationService) {
        this.loanRepository = loanRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Loan applyForLoan(User user, LoanRequest request) {
        LoanType type;
        try {
            type = LoanType.valueOf(request.getLoanType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidTransactionException("Invalid Loan Type: Choose PERSONAL, HOME, BIKE, CAR, or GOLD");
        }

        // Fixed 10% interest rate for all loan types as per requirements
        BigDecimal interestRate = new BigDecimal("10.0");

        // Generate custom loan ID if not provided or to ensure format
        String acctNo = request.getAccountNumber() != null ? request.getAccountNumber().trim() : "";
        String last4 = acctNo.length() >= 4 ? acctNo.substring(acctNo.length() - 4) : acctNo;
        String generatedLoanId = (request.getCustomLoanId() != null && !request.getCustomLoanId().isBlank()) 
                ? request.getCustomLoanId() 
                : "MASHA" + last4;

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanType(type);
        loan.setAccountNumber(acctNo);
        loan.setCustomLoanId(generatedLoanId);
        loan.setRequestedAmount(request.getRequestedAmount());
        loan.setLoanDuration(request.getLoanDuration());
        loan.setInterestRate(interestRate);
        loan.setProofNumber(request.getProofNumber());
        loan.setEmi(request.getEmi());
        loan.setGoldWeight(request.getGoldWeight());
        loan.setGoldPurity(request.getGoldPurity());
        loan.setLoanStatus(LoanStatus.PENDING);

        Loan savedLoan = loanRepository.save(loan);

        notificationService.createNotification(
                user,
                "Loan Application Submitted",
                "Your application for a ₹" + savedLoan.getRequestedAmount() + " " + savedLoan.getLoanType().name() + " loan (ID: " + savedLoan.getCustomLoanId() + ") has been submitted. Status: PENDING.",
                NotificationType.LOAN
        );

        return savedLoan;
    }

    @Override
    @Transactional(readOnly = true)
    public Loan getLoanById(Long id, User user) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        // Security check: normal user can only view their own loan
        if (user.getRole() == Role.ROLE_USER && !loan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access Denied: You do not own this loan application.");
        }

        return loan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getLoansByUser(User user) {
        return loanRepository.findByUserIdOrderByApplicationDateDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getLoansByUserId(Long userId) {
        return loanRepository.findByUserIdOrderByApplicationDateDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        return loanRepository.findAllByOrderByApplicationDateDesc();
    }

    @Override
    public void approveLoan(Long id, String adminRemarks) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        if (loan.getLoanStatus() != LoanStatus.PENDING) {
            throw new InvalidTransactionException("Loan status is already: " + loan.getLoanStatus().name());
        }

        loan.setLoanStatus(LoanStatus.APPROVED);
        loan.setApprovedRejectedDate(LocalDateTime.now());
        loan.setAdminRemarks(adminRemarks);
        loanRepository.save(loan);

        notificationService.createNotification(
                loan.getUser(),
                "Loan Approved",
                "Congratulations! Your application for a $" + loan.getRequestedAmount() + " " + loan.getLoanType().name() + " loan has been APPROVED. Remarks: " + (adminRemarks != null ? adminRemarks : "None"),
                NotificationType.LOAN
        );
    }

    @Override
    public void rejectLoan(Long id, String adminRemarks) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + id));

        if (loan.getLoanStatus() != LoanStatus.PENDING) {
            throw new InvalidTransactionException("Loan status is already: " + loan.getLoanStatus().name());
        }

        loan.setLoanStatus(LoanStatus.REJECTED);
        loan.setApprovedRejectedDate(LocalDateTime.now());
        loan.setAdminRemarks(adminRemarks);
        loanRepository.save(loan);

        notificationService.createNotification(
                loan.getUser(),
                "Loan Rejected",
                "We regret to inform you that your application for a $" + loan.getRequestedAmount() + " " + loan.getLoanType().name() + " loan has been REJECTED. Remarks: " + (adminRemarks != null ? adminRemarks : "None"),
                NotificationType.LOAN
        );
    }
}
