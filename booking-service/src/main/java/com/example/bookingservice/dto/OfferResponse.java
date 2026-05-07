package com.example.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OfferResponse(
        Long id,
        Long providerId,
        String title,
        String description,
        BigDecimal price,
        Boolean available,
        String category,
        LocalDateTime availableDateTime,
        String availabilityStatus
) {
}
