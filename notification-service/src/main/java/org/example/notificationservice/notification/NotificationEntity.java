package org.example.notificationservice.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 1000)
    private String message;

    private Long customerId;

    private Long serviceProviderId;

    private Long bookingId;

    private String serviceName;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private String status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientType recipientType;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationEntity() {
    }

    public NotificationEntity(
            NotificationType type,
            String message,
            Long customerId,
            Long serviceProviderId,
            Long bookingId,
            String serviceName,
            BigDecimal amount,
            String status,
            RecipientType recipientType,
            Long recipientId
    ) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.message = message;
        this.customerId = customerId;
        this.serviceProviderId = serviceProviderId;
        this.bookingId = bookingId;
        this.serviceName = serviceName;
        this.amount = amount;
        this.status = status;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public NotificationResponse toResponse() {
        return new NotificationResponse(
                id,
                type,
                message,
                customerId,
                serviceProviderId,
                bookingId,
                serviceName,
                amount,
                status,
                recipientType,
                recipientId,
                createdAt
        );
    }
}
