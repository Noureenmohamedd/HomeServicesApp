package org.example.notificationservice.notification;

import java.math.BigDecimal;

public record NotificationMessage(
        String eventType,
        Long bookingId,
        Long customerId,
        Long providerId,
        Long offerId,
        String serviceName,
        String category,
        String status,
        BigDecimal amount,
        String message
) {
    public Long serviceProviderId() {
        return providerId;
    }

    public NotificationType type() {
        if ("BOOKING_COMPLETED".equalsIgnoreCase(eventType)) {
            return NotificationType.BOOKING_COMPLETION;
        }
        if ("BOOKING_REJECTED".equalsIgnoreCase(eventType) || "PAYMENT_FAILED".equalsIgnoreCase(eventType)) {
            return NotificationType.BOOKING_REJECTION;
        }
        return NotificationType.BOOKING_CONFIRMATION;
    }
}
