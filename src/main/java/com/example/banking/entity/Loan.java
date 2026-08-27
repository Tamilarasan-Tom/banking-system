package com.example.banking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "loan_type")
    private LoanType loanType;

    @Column(nullable = false, name = "requested_amount")
    private BigDecimal requestedAmount;

    @Column(nullable = false, name = "interest_rate")
    private BigDecimal interestRate;

    @Column(nullable = false, name = "loan_duration")
    private Integer loanDuration; // in months

    @Column(nullable = false, name = "application_date")
    private LocalDateTime applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "loan_status")
    private LoanStatus loanStatus;

    @Column(name = "approved_rejected_date")
    private LocalDateTime approvedRejectedDate;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "custom_loan_id")
    private String customLoanId;

    @Column(name = "proof_number")
    private String proofNumber;

    @Column(name = "emi")
    private BigDecimal emi;

    @Column(name = "gold_weight")
    private Integer goldWeight;

    @Column(name = "gold_purity")
    private String goldPurity;

    @Column(name = "admin_remarks")
    private String adminRemarks;

    @PrePersist
    protected void onCreate() {
        applicationDate = LocalDateTime.now();
        if (loanStatus == null) {
            loanStatus = LoanStatus.PENDING;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getLoanDuration() {
        return loanDuration;
    }

    public void setLoanDuration(Integer loanDuration) {
        this.loanDuration = loanDuration;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LoanStatus getLoanStatus() {
        return loanStatus;
    }

    public void setLoanStatus(LoanStatus loanStatus) {
        this.loanStatus = loanStatus;
    }

    public LocalDateTime getApprovedRejectedDate() {
        return approvedRejectedDate;
    }

    public void setApprovedRejectedDate(LocalDateTime approvedRejectedDate) {
        this.approvedRejectedDate = approvedRejectedDate;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCustomLoanId() {
        return customLoanId;
    }

    public void setCustomLoanId(String customLoanId) {
        this.customLoanId = customLoanId;
    }

    public String getProofNumber() {
        return proofNumber;
    }

    public void setProofNumber(String proofNumber) {
        this.proofNumber = proofNumber;
    }

    public BigDecimal getEmi() {
        return emi;
    }

    public void setEmi(BigDecimal emi) {
        this.emi = emi;
    }

    public Integer getGoldWeight() {
        return goldWeight;
    }

    public void setGoldWeight(Integer goldWeight) {
        this.goldWeight = goldWeight;
    }

    public String getGoldPurity() {
        return goldPurity;
    }

    public void setGoldPurity(String goldPurity) {
        this.goldPurity = goldPurity;
    }
}
