package com.example.bookingservice.dto;

import com.example.bookingservice.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long bookingId,
        Long customerId,
        String customerName,
        Long providerId,
        Long offerId,
        String title,
        String category,
        BigDecimal amount,
        BookingStatus status,
        LocalDateTime bookingDate,
        LocalDateTime availableDateTime
) {
}
