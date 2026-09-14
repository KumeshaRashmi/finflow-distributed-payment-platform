package com.finflow.account_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Owner name is required")
        String ownerName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Balance cannot be negative")
        BigDecimal initialBalance,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be 3 letters")
        String currency
) {
}