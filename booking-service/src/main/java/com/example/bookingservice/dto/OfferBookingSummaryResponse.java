package com.example.bookingservice.dto;

import com.example.bookingservice.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfferBookingSummaryResponse(
        Long bookingId,
        Long customerId,
        String customerName,
        BookingStatus status,
        BigDecimal amount,
        LocalDateTime bookingDate
) {
}
