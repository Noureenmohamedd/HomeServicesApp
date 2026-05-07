package com.example.offerservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OfferResponse {

    private Long id;
    private Long providerId;
    private String title;
    private String description;
    private BigDecimal price;
    private boolean available;
}
