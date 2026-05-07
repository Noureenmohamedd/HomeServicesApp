package com.example.offerservice.controller;

import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.config.AuthenticatedUser;
import com.example.offerservice.service.OfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody CreateOfferRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOffer(request, user.userId()));
    }

    @GetMapping
    public ResponseEntity<List<OfferResponse>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOfferById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<OfferResponse>> getOffersByProviderId(@PathVariable @Positive Long providerId) {
        return ResponseEntity.ok(offerService.getOffersByProviderId(providerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferResponse> updateOffer(@PathVariable @Positive Long id,
                                                     @Valid @RequestBody UpdateOfferRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(offerService.updateOffer(id, request, user.userId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable @Positive Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}
