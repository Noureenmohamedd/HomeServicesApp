package com.example.bookingservice.controller;

import com.example.bookingservice.config.AuthenticatedUser;
import com.example.bookingservice.dto.CompletedProviderOfferResponse;
import com.example.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceHistoryController {

    private final BookingService bookingService;

    @GetMapping("/completed")
    public ResponseEntity<List<CompletedProviderOfferResponse>> getCompletedServices(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getCompletedProviderBookings(
                authenticatedUser.userId(),
                authenticatedUser
        ));
    }
}
