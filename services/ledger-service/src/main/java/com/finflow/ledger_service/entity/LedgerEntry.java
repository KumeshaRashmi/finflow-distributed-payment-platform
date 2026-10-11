package com.finflow.ledger_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransaction transaction;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public enum EntryType {
        DEBIT,
        CREDIT
    }

    public LedgerEntry() {
    }

    public Long getId() {
        return id;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(LedgerTransaction transaction) {
        this.transaction = transaction;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}