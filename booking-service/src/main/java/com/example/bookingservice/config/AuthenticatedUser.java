package com.example.bookingservice.config;

public record AuthenticatedUser(
        Long userId,
        String role,
        String displayName,
        String professionType
) {
}
