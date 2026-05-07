package com.example.bookingservice.controller;

import com.example.bookingservice.config.AuthenticatedUser;
import com.example.bookingservice.dto.AdminBookingResponse;
import com.example.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping("/bookings")
    public ResponseEntity<List<AdminBookingResponse>> getAllBookingHistory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getAllBookingHistory(authenticatedUser));
    }
}
