package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull(message = "offerId is required")
        Long offerId
) {
}
