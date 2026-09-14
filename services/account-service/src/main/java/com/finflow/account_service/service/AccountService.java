package com.finflow.account_service.service;

import com.finflow.account_service.dto.AccountResponse;
import com.finflow.account_service.dto.CreateAccountRequest;
import com.finflow.account_service.entity.Account;
import com.finflow.account_service.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount(CreateAccountRequest request) {

        // Check whether email already exists
        if (accountRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        // Create Account entity
        Account account = new Account();

        account.setAccountNumber(generateAccountNumber());
        account.setOwnerName(request.ownerName());
        account.setEmail(request.email());
        account.setBalance(request.initialBalance());
        account.setCurrency(request.currency().toUpperCase());
        account.setStatus("ACTIVE");

        // Save account to PostgreSQL
        Account savedAccount = accountRepository.save(account);

        // Convert Entity to Response DTO
        return toResponse(savedAccount);
    }

    public AccountResponse getAccount(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found"
                ));

        return toResponse(account);
    }

    private String generateAccountNumber() {

        return "ACC-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private AccountResponse toResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getEmail(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}