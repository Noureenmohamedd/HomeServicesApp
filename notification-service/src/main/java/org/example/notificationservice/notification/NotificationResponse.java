package org.example.notificationservice.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        Long customerId,
        Long serviceProviderId,
        Long bookingId,
        String serviceName,
        BigDecimal amount,
        String status,
        RecipientType recipientType,
        Long recipientId,
        Instant createdAt
) {
}
