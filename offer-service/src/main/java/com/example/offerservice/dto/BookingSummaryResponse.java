package com.example.offerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingSummaryResponse(
        Long bookingId,
        Long customerId,
        String customerName,
        String status,
        BigDecimal amount,
        LocalDateTime bookingDate
) {
}
