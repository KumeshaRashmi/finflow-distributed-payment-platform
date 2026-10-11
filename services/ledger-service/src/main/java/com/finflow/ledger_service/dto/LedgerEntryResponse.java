package com.finflow.ledger_service.dto;

import java.math.BigDecimal;

public record LedgerEntryResponse(
        Long id,
        Long accountId,
        String type,
        BigDecimal amount
) {
}