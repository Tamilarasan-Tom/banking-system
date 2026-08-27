package com.example.banking.dto;

import java.math.BigDecimal;

public class AdminStatsResponse {
    private long totalUsers;
    private long totalAccounts;
    private long activeAccounts;
    private long totalTransactions;
    private BigDecimal totalTransferAmount;
    private long pendingLoans;
    private long approvedLoans;
    private long rejectedLoans;

    public AdminStatsResponse() {
        this.totalTransferAmount = BigDecimal.ZERO;
    }

    public AdminStatsResponse(long totalUsers, long totalAccounts, long activeAccounts, long totalTransactions, BigDecimal totalTransferAmount, long pendingLoans, long approvedLoans, long rejectedLoans) {
        this.totalUsers = totalUsers;
        this.totalAccounts = totalAccounts;
        this.activeAccounts = activeAccounts;
        this.totalTransactions = totalTransactions;
        this.totalTransferAmount = totalTransferAmount != null ? totalTransferAmount : BigDecimal.ZERO;
        this.pendingLoans = pendingLoans;
        this.approvedLoans = approvedLoans;
        this.rejectedLoans = rejectedLoans;
    }

    // Getters and Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getActiveAccounts() {
        return activeAccounts;
    }

    public void setActiveAccounts(long activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalTransferAmount() {
        return totalTransferAmount;
    }

    public void setTotalTransferAmount(BigDecimal totalTransferAmount) {
        this.totalTransferAmount = totalTransferAmount != null ? totalTransferAmount : BigDecimal.ZERO;
    }

    public long getPendingLoans() {
        return pendingLoans;
    }

    public void setPendingLoans(long pendingLoans) {
        this.pendingLoans = pendingLoans;
    }

    public long getApprovedLoans() {
        return approvedLoans;
    }

    public void setApprovedLoans(long approvedLoans) {
        this.approvedLoans = approvedLoans;
    }

    public long getRejectedLoans() {
        return rejectedLoans;
    }

    public void setRejectedLoans(long rejectedLoans) {
        this.rejectedLoans = rejectedLoans;
    }
}
