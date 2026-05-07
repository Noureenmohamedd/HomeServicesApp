package com.example.bookingservice.dto;

import com.example.bookingservice.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long customerId,
        String customerName,
        Long offerId,
        Long providerId,
        String title,
        String category,
        BigDecimal amount,
        BookingStatus status,
        String statusMeaning,
        LocalDateTime bookingDate,
        LocalDateTime availableDateTime
) {
}
