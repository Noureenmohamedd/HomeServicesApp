package com.example.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompletedCustomerResponse(
        Long bookingId,
        Long customerId,
        String customerName,
        BigDecimal amount,
        LocalDateTime bookingDate
) {
}
