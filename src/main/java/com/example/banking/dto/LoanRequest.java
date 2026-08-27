package com.example.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class LoanRequest {

    @NotBlank(message = "Loan type is required (PERSONAL, HOME, BIKE, CAR, or GOLD)")
    private String loanType;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    private String customLoanId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1.0", message = "Requested amount must be valid")
    private BigDecimal requestedAmount;

    @NotNull(message = "Loan duration in months is required")
    @Min(value = 1, message = "Loan duration must be at least 1 month")
    private Integer loanDuration;

    @NotBlank(message = "Proof / Aadhar number is required")
    private String proofNumber;

    private BigDecimal emi;
    private Integer goldWeight;
    private String goldPurity;

    // Getters and Setters
    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
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

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getLoanDuration() {
        return loanDuration;
    }

    public void setLoanDuration(Integer loanDuration) {
        this.loanDuration = loanDuration;
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
