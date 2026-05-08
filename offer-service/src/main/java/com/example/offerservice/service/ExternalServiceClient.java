package com.example.offerservice.service;

import com.example.offerservice.dto.BookingSummaryResponse;
import com.example.offerservice.dto.CompletedProviderOfferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.booking-service.base-url:http://localhost:8082}")
    private String bookingServiceBaseUrl;

    @Value("${services.user-service.base-url:http://localhost:8080}")
    private String userServiceBaseUrl;

    @Value("${services.user-service.user-details-path:/user-service-ejb-1.0-SNAPSHOT/api/users/{userId}}")
    private String userDetailsPath;

    public List<BookingSummaryResponse> fetchBookingsForOffer(Long offerId, String authorizationHeader) {
        String url = bookingServiceBaseUrl + "/api/bookings/offer/{offerId}";
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(authorizationHeader)),
                    new ParameterizedTypeReference<List<BookingSummaryResponse>>() {
                    },
                    offerId
            ).getBody();
        } catch (RestClientException exception) {
            log.warn("Unable to fetch bookings for offerId={}: {}", offerId, exception.getMessage());
            return List.of();
        }
    }

    public ProviderProfile fetchProviderProfile(Long providerId, String authorizationHeader) {
        String url = userServiceBaseUrl + userDetailsPath;
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(authorizationHeader)),
                    ProviderProfile.class,
                    providerId
            ).getBody();
        } catch (RestClientException exception) {
            log.warn("Unable to fetch provider profile providerId={}: {}", providerId, exception.getMessage());
            return null;
        }
    }

    public List<CompletedProviderOfferResponse> fetchCompletedProviderOffers(
            Long providerId,
            String authorizationHeader
    ) {
        String url = bookingServiceBaseUrl + "/api/bookings/provider/{providerId}/completed";
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(authorizationHeader)),
                    new ParameterizedTypeReference<List<CompletedProviderOfferResponse>>() {
                    },
                    providerId
            ).getBody();
        } catch (RestClientException exception) {
            log.warn("Unable to fetch completed provider offers providerId={}: {}", providerId, exception.getMessage());
            return List.of();
        }
    }

    private HttpHeaders authHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return headers;
    }

    public record ProviderProfile(
            Long id,
            String username,
            String role,
            String professionType
    ) {
    }
}
