package com.example.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TransactionPinRequest {

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "\\d{4}", message = "Transaction PIN must be exactly 4 digits")
    private String pin;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}