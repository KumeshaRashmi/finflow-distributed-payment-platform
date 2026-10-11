package com.finflow.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull(message = "Source account ID is required")
        @Positive(message = "Source account ID must be positive")
        Long sourceAccountId,

        @NotNull(message = "Destination account ID is required")
        @Positive(message = "Destination account ID must be positive")
        Long destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Payment amount must be at least 0.01"
        )
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency must contain three letters"
        )
        String currency,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
}