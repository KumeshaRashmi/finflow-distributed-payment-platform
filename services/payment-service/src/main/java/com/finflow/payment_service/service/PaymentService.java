package com.finflow.payment_service.service;

import com.finflow.payment_service.dto.CreatePaymentRequest;
import com.finflow.payment_service.dto.PaymentResponse;
import com.finflow.payment_service.entity.Payment;
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

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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

        // Return the existing payment when a request is retried.
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

        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
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