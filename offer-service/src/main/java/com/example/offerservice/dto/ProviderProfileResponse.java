package com.example.offerservice.dto;

public record ProviderProfileResponse(
        Long id,
        String username,
        String role,
        String professionType
) {
}
