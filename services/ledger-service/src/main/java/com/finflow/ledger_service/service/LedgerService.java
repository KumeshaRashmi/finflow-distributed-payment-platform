package com.finflow.ledger_service.service;

import com.finflow.ledger_service.dto.CreateLedgerTransactionRequest;
import com.finflow.ledger_service.dto.LedgerEntryResponse;
import com.finflow.ledger_service.dto.LedgerTransactionResponse;
import com.finflow.ledger_service.entity.LedgerEntry;
import com.finflow.ledger_service.entity.LedgerTransaction;
import com.finflow.ledger_service.repository.LedgerTransactionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class LedgerService {

    private final LedgerTransactionRepository repository;

    public LedgerService(LedgerTransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LedgerTransactionResponse createTransaction(
            CreateLedgerTransactionRequest request) {

        if (request.sourceAccountId().equals(
                request.destinationAccountId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination accounts must differ"
            );
        }

        // Idempotency: do not create the same ledger transaction twice.
        var existing = repository.findByPaymentReference(
                request.paymentReference()
        );

        if (existing.isPresent()) {
            LedgerTransaction transaction = existing.get();

            boolean sameRequest =
                    transaction.getAmount().compareTo(request.amount()) == 0
                            && transaction.getCurrency().equalsIgnoreCase(
                            request.currency())
                            && hasExpectedAccounts(
                            transaction,
                            request.sourceAccountId(),
                            request.destinationAccountId()
                    );

            if (!sameRequest) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment reference already exists with different details"
                );
            }

            return toResponse(transaction);
        }

        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setPaymentReference(request.paymentReference());
        transaction.setAmount(request.amount());
        transaction.setCurrency(
                request.currency().toUpperCase(Locale.ROOT)
        );

        LedgerEntry debit = new LedgerEntry();
        debit.setAccountId(request.sourceAccountId());
        debit.setType(LedgerEntry.EntryType.DEBIT);
        debit.setAmount(request.amount());

        LedgerEntry credit = new LedgerEntry();
        credit.setAccountId(request.destinationAccountId());
        credit.setType(LedgerEntry.EntryType.CREDIT);
        credit.setAmount(request.amount());

        transaction.addEntry(debit);
        transaction.addEntry(credit);

        // Both entries are persisted in the same database transaction.
        LedgerTransaction saved = repository.save(transaction);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LedgerTransactionResponse getByPaymentReference(
            String paymentReference) {

        LedgerTransaction transaction =
                repository.findByPaymentReference(paymentReference)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ledger transaction not found"
                        ));

        return toResponse(transaction);
    }

    private boolean hasExpectedAccounts(
            LedgerTransaction transaction,
            Long sourceAccountId,
            Long destinationAccountId) {

        boolean hasDebit = transaction.getEntries().stream()
                .anyMatch(entry ->
                        entry.getAccountId().equals(sourceAccountId)
                                && entry.getType() == LedgerEntry.EntryType.DEBIT);

        boolean hasCredit = transaction.getEntries().stream()
                .anyMatch(entry ->
                        entry.getAccountId().equals(destinationAccountId)
                                && entry.getType() == LedgerEntry.EntryType.CREDIT);

        return hasDebit && hasCredit;
    }

    private LedgerTransactionResponse toResponse(
            LedgerTransaction transaction) {

        List<LedgerEntryResponse> entries =
                transaction.getEntries().stream()
                        .map(entry -> new LedgerEntryResponse(
                                entry.getId(),
                                entry.getAccountId(),
                                entry.getType().name(),
                                entry.getAmount()
                        ))
                        .toList();

        // Verify the entries balance before returning the result.
        BigDecimal debits = transaction.getEntries().stream()
                .filter(entry ->
                        entry.getType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credits = transaction.getEntries().stream()
                .filter(entry ->
                        entry.getType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debits.compareTo(credits) != 0) {
            throw new IllegalStateException(
                    "Ledger transaction is not balanced"
            );
        }

        return new LedgerTransactionResponse(
                transaction.getId(),
                transaction.getPaymentReference(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCreatedAt(),
                entries
        );
    }
}