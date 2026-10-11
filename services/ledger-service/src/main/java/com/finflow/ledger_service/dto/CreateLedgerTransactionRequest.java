package com.finflow.ledger_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateLedgerTransactionRequest(

        @NotBlank(message = "Payment reference is required")
        String paymentReference,

        @NotNull(message = "Sender account ID is required")
        @Positive(message = "Sender account ID must be positive")
        Long sourceAccountId,

        @NotNull(message = "Receiver account ID is required")
        @Positive(message = "Receiver account ID must be positive")
        Long destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be positive"
        )
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "^[A-Za-z]{3}$",
                message = "Currency must contain three letters"
        )
        String currency
) {
}