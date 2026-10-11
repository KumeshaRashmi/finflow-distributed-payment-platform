package com.finflow.ledger_service.controller;

import com.finflow.ledger_service.dto.CreateLedgerTransactionRequest;
import com.finflow.ledger_service.dto.LedgerTransactionResponse;
import com.finflow.ledger_service.service.LedgerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger/transactions")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<LedgerTransactionResponse> createTransaction(
            @Valid @RequestBody CreateLedgerTransactionRequest request) {

        LedgerTransactionResponse response =
                ledgerService.createTransaction(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{paymentReference}")
    public ResponseEntity<LedgerTransactionResponse> getTransaction(
            @PathVariable String paymentReference) {

        return ResponseEntity.ok(
                ledgerService.getByPaymentReference(paymentReference)
        );
    }
}