package com.example.bookingservice.controller;

import com.example.bookingservice.config.AuthenticatedUser;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CompletedProviderOfferResponse;
import com.example.bookingservice.dto.CreateBookingRequest;
import com.example.bookingservice.dto.OfferBookingSummaryResponse;
import com.example.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        BookingResponse response = bookingService.createBooking(request, authenticatedUser, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<BookingResponse>> getAllBookingsForAdmin(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getAllBookingsForAdmin(authenticatedUser));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingResponse>> getCustomerBookings(
            @PathVariable Long customerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getCustomerBookings(customerId, authenticatedUser));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<BookingResponse>> getProviderBookings(
            @PathVariable Long providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getProviderBookings(providerId, authenticatedUser));
    }

    @GetMapping("/provider/{providerId}/completed")
    public ResponseEntity<List<CompletedProviderOfferResponse>> getCompletedProviderBookings(
            @PathVariable Long providerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getCompletedProviderBookings(providerId, authenticatedUser));
    }

    @GetMapping("/offer/{offerId}")
    public ResponseEntity<List<OfferBookingSummaryResponse>> getOfferBookings(
            @PathVariable Long offerId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.getOfferBookings(offerId, authenticatedUser));
    }

    @PutMapping("/{bookingId}/complete")
    public ResponseEntity<BookingResponse> completeBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(bookingService.completeBooking(bookingId, authenticatedUser));
    }
}
