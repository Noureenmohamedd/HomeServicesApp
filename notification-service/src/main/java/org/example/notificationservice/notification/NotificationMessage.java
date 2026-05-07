package org.example.notificationservice.notification;

import java.math.BigDecimal;

public record NotificationMessage(
        NotificationType type,
        String message,
        Long customerId,
        Long serviceProviderId,
        Long bookingId,
        String serviceName,
        BigDecimal amount,
        String status
) {
}
