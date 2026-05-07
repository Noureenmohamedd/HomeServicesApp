package com.example.offerservice.config;

public record AuthenticatedUser(Long userId, String username, String role, String professionType) {
}
