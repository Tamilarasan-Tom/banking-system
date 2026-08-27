package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;

public class AccountStatusRequest {

    @NotBlank(message = "Status is required (ACTIVE, BLOCKED, or CLOSED)")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
