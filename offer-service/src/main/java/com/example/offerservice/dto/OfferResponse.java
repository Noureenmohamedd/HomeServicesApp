package com.example.offerservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OfferResponse {

    private Long id;
    private Long providerId;
    private String providerUsername;
    private String providerProfessionType;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private LocalDateTime availableDateTime;
    private String availabilityStatus;
}
