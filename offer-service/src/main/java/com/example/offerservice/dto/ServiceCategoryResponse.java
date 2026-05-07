package com.example.offerservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceCategoryResponse {

    private Long id;
    private String name;
    private String professionType;
    private boolean active;
}
