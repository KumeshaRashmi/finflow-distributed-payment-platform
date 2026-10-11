package com.finflow.payment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.payment_service.dto.CreatePaymentRequest;
import com.finflow.payment_service.dto.PaymentResponse;
import com.finflow.payment_service.entity.Payment;
import com.finflow.payment_service.outbox.OutboxEvent;
import com.finflow.payment_service.outbox.OutboxEventRepository;
import com.finflow.payment_service.outbox.PaymentRequestedEvent;
import com.finflow.payment_service.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {

        if (request.sourceAccountId()
                .equals(request.destinationAccountId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination accounts must be different"
            );
        }

        var existing = paymentRepository.findByIdempotencyKey(
                request.idempotencyKey()
        );

        if (existing.isPresent()) {
            Payment payment = existing.get();

            boolean sameRequest =
                    payment.getSourceAccountId().equals(
                            request.sourceAccountId())
                            && payment.getDestinationAccountId().equals(
                            request.destinationAccountId())
                            && payment.getAmount().compareTo(
                            request.amount()) == 0
                            && payment.getCurrency().equalsIgnoreCase(
                            request.currency());

            if (!sameRequest) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key was already used for a different payment"
                );
            }

            // A retry returns the existing payment; it creates no new event.
            return toResponse(payment);
        }

        Payment payment = new Payment();

        payment.setPaymentReference(
                "PAY-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase(Locale.ROOT)
        );

        payment.setSourceAccountId(request.sourceAccountId());
        payment.setDestinationAccountId(request.destinationAccountId());
        payment.setAmount(request.amount());
        payment.setCurrency(
                request.currency().toUpperCase(Locale.ROOT)
        );
        payment.setStatus("PENDING");
        payment.setIdempotencyKey(request.idempotencyKey());

        Payment savedPayment = paymentRepository.save(payment);

        String eventId = UUID.randomUUID().toString();

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId,
                "PAYMENT_REQUESTED",
                savedPayment.getId().toString(),
                savedPayment.getPaymentReference(),
                savedPayment.getSourceAccountId(),
                savedPayment.getDestinationAccountId(),
                savedPayment.getAmount().toPlainString(),
                savedPayment.getCurrency()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            outboxRepository.save(new OutboxEvent(
                    eventId,
                    savedPayment.getId().toString(),
                    "PAYMENT_REQUESTED",
                    payload
            ));
        } catch (JsonProcessingException ex) {
            // Throwing causes the payment transaction to roll back too.
            throw new IllegalStateException(
                    "Could not serialize payment event", ex
            );
        }

        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"
                ));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getSourceAccountId(),
                payment.getDestinationAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}