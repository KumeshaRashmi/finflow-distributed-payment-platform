package com.finflow.payment_service.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        var events = repository.findByPublishedAtIsNullOrderByIdAsc(
                PageRequest.of(0, 50)
        );

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        "payment-requests",
                        event.getAggregateId(),
                        event.getPayload()
                ).get(10, TimeUnit.SECONDS);

                markPublished(event.getId());

            } catch (Exception ex) {
                // Leave this event unpublished so a later poll retries it.
                System.err.println(
                        "Could not publish outbox event "
                                + event.getEventId() + ": "
                                + ex.getMessage()
                );
            }
        }
    }

    @Transactional
    public void markPublished(Long id) {
        repository.findById(id).ifPresent(event -> {
            event.markPublished();
            repository.save(event);
        });
    }
}