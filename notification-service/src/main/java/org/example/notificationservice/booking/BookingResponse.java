package org.example.notificationservice.booking;

import java.math.BigDecimal;

public record BookingResponse(
        Long bookingId,
        Long customerId,
        Long serviceProviderId,
        String serviceName,
        BigDecimal price,
        BigDecimal remainingWalletBalance,
        BookingStatus status,
        String message
) {
}
