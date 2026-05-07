package com.example.bookingservice.messaging;

import com.example.bookingservice.entity.BookingStatus;

import java.math.BigDecimal;

public record BookingEvent(
        Long bookingId,
        Long customerId,
        Long providerId,
        BookingStatus status,
        BigDecimal amount,
        String eventType
) {
}
