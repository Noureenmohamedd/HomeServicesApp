package com.example.bookingservice.dto;

import com.example.bookingservice.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompletedBookingResponse(
        Long bookingId,
        Long customerId,
        String customerUsername,
        Long offerId,
        String title,
        String category,
        BigDecimal amount,
        LocalDateTime bookingDate,
        LocalDateTime availableDateTime,
        BookingStatus status
) {
}
