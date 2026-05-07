package com.example.offerservice.controller;

import com.example.offerservice.config.AuthenticatedUser;
import com.example.offerservice.dto.CompletedProviderOfferResponse;
import com.example.offerservice.service.ExternalServiceClient;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.logging.Logger;

@Validated
@RestController
@RequestMapping("/api/bookings")
public class BookingProxyController {

    private static final Logger LOGGER = Logger.getLogger(BookingProxyController.class.getName());

    private final ExternalServiceClient externalServiceClient;

    public BookingProxyController(ExternalServiceClient externalServiceClient) {
        this.externalServiceClient = externalServiceClient;
    }

    @GetMapping("/provider/{providerId}/completed")
    public ResponseEntity<List<CompletedProviderOfferResponse>> getCompletedProviderOffers(
            @PathVariable @Positive Long providerId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (!"PROVIDER".equalsIgnoreCase(user.role()) || !providerId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider can only view his own completed services");
        }

        LOGGER.info("Proxy completed provider bookings request: providerId=" + providerId + ", userId=" + user.userId());
        return ResponseEntity.ok(externalServiceClient.fetchCompletedProviderOffers(providerId, authorizationHeader));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user;
    }
}
