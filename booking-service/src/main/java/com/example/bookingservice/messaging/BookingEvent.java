package com.example.bookingservice.messaging;

import com.example.bookingservice.entity.BookingStatus;

import java.math.BigDecimal;

public record BookingEvent(
        String eventType,
        Long bookingId,
        Long customerId,
        Long providerId,
        Long offerId,
        String serviceName,
        String category,
        BookingStatus status,
        BigDecimal amount,
        String message
) {
}
