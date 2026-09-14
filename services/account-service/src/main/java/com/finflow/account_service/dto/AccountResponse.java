package com.finflow.account_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(

        Long id,
        String accountNumber,
        String ownerName,
        String email,
        BigDecimal balance,
        String currency,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}