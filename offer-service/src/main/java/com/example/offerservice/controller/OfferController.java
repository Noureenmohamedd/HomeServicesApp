package com.example.offerservice.controller;

import com.example.offerservice.dto.ActiveOfferResponse;
import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.config.AuthenticatedUser;
import com.example.offerservice.service.OfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.logging.Logger;

@Validated
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private static final Logger LOGGER = Logger.getLogger(OfferController.class.getName());

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody CreateOfferRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Create offer request: providerId=" + user.userId()
                + ", username=" + user.username()
                + ", professionType=" + user.professionType()
                + ", category=" + request.getCategory()
                + ", availableDateTime=" + request.getAvailableDateTime());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offerService.createOffer(request, user.userId(), user.username(), user.professionType()));
    }

    @GetMapping
    public ResponseEntity<List<OfferResponse>> getAllOffers() {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("All offers requested: userId=" + user.userId() + ", role=" + user.role());
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOfferById(@PathVariable @Positive Long id) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Offer details requested: userId=" + user.userId() + ", offerId=" + id);
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<OfferResponse>> getOffersByProviderId(@PathVariable @Positive Long providerId) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Provider offers requested: userId=" + user.userId()
                + ", role=" + user.role()
                + ", providerId=" + providerId);
        return ResponseEntity.ok(offerService.getOffersByProviderId(providerId, user.userId(), user.role()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<OfferResponse>> getOffersByCategory(@PathVariable String category) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Future offer category search requested: userId=" + user.userId()
                + ", role=" + user.role()
                + ", category=" + category);
        return ResponseEntity.ok(offerService.getOffersByCategory(category));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ActiveOfferResponse>> getActiveOffers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Active offers requested: userId=" + user.userId() + ", role=" + user.role());
        return ResponseEntity.ok(offerService.getActiveOffers(user.userId(), user.role(), authorizationHeader));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferResponse> updateOffer(@PathVariable @Positive Long id,
                                                     @Valid @RequestBody UpdateOfferRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Update offer request: userId=" + user.userId()
                + ", offerId=" + id
                + ", availableDateTime=" + request.getAvailableDateTime());
        return ResponseEntity.ok(offerService.updateOffer(id, request, user.userId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable @Positive Long id) {
        AuthenticatedUser user = getAuthenticatedUser();
        LOGGER.info("Delete offer request: userId=" + user.userId() + ", offerId=" + id);
        offerService.deleteOffer(id, user.userId());
        return ResponseEntity.noContent().build();
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
