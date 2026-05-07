package com.example.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompletedProviderOfferResponse(
        Long offerId,
        String title,
        String category,
        BigDecimal price,
        LocalDateTime availableDateTime,
        List<CompletedCustomerResponse> completedCustomers
) {
}
