package com.finflow.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentReference,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}