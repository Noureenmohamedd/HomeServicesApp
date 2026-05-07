package com.example.offerservice.config;

public record JwtClaims(Long userId, String username, String role, String professionType) {
}
