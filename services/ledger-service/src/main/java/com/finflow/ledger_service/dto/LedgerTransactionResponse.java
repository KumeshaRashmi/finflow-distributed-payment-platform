package com.finflow.ledger_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LedgerTransactionResponse(
        Long id,
        String paymentReference,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt,
        List<LedgerEntryResponse> entries
) {
}