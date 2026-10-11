package com.finflow.payment_service.outbox;

public record PaymentRequestedEvent(
        String eventId,
        String eventType,
        String paymentId,
        String paymentReference,
        Long sourceAccountId,
        Long destinationAccountId,
        String amount,
        String currency
) {
}