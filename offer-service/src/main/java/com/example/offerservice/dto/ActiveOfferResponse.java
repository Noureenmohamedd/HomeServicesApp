package com.example.offerservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ActiveOfferResponse {

    private Long id;
    private Long offerId;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private boolean available;
    private String availabilityStatus;
    private LocalDateTime availableDateTime;
    private Long providerId;
    private String providerUsername;
    private String providerProfessionType;
    private List<BookingSummaryResponse> bookings;
}
