package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;

public class UserStatusRequest {
    
    @NotBlank(message = "Status is required (ACTIVE or BLOCKED)")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
