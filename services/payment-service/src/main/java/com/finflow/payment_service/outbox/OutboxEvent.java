package com.finflow.payment_service.outbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_outbox",
        indexes = @Index(
                name = "idx_payment_outbox_unpublished",
                columnList = "published_at"
        )
)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String aggregateId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            String eventId,
            String aggregateId,
            String eventType,
            String payload) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void markPublished() {
        publishedAt = LocalDateTime.now();
    }
}