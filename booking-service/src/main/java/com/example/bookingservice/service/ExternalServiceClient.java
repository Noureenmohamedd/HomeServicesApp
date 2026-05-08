package com.example.bookingservice.service;

import com.example.bookingservice.dto.DeductWalletRequest;
import com.example.bookingservice.dto.OfferResponse;
import com.example.bookingservice.dto.WalletBalanceResponse;
import com.example.bookingservice.exception.InsufficientBalanceException;
import com.example.bookingservice.exception.PaymentException;
import com.example.bookingservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.offer-service.base-url}")
    private String offerServiceBaseUrl;

    @Value("${services.user-service.base-url}")
    private String userServiceBaseUrl;

    @Value("${services.user-service.wallet-balance-path}")
    private String walletBalancePath;

    @Value("${services.user-service.wallet-deduct-path}")
    private String walletDeductPath;

    @Value("${services.user-service.wallet-refund-path}")
    private String walletRefundPath;

    public OfferResponse fetchOffer(Long offerId, String authorizationHeader) {
        log.info("FETCHING OFFER offerId={}", offerId);
        String url = offerServiceBaseUrl + "/api/offers/{offerId}";
        try {
            String payload = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(authorizationHeader)),
                    String.class,
                    offerId
            ).getBody();
            OfferResponse offer = parseOffer(payload, offerId);
            log.info("OFFER FETCHED offerId={} providerId={} availabilityStatus={} availableDateTime={} legacyAvailable={}",
                    offer == null ? null : offer.id(),
                    offer == null ? null : offer.providerId(),
                    offer == null ? null : offer.availabilityStatus(),
                    offer == null ? null : offer.availableDateTime(),
                    offer == null ? null : offer.available());
            return offer;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Offer not found: " + offerId);
        } catch (HttpStatusCodeException ex) {
            log.warn("OFFER SERVICE ERROR offerId={} status={} body={}", offerId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResourceNotFoundException("Unable to fetch offer " + offerId + ": offer-service returned " + ex.getStatusCode());
        } catch (RestClientException ex) {
            log.warn("OFFER SERVICE CALL FAILED offerId={} message={}", offerId, ex.getMessage());
            throw new ResourceNotFoundException("Unable to fetch offer " + offerId + ": " + ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("OFFER RESPONSE PARSE FAILED offerId={} message={}", offerId, ex.getMessage());
            throw new ResourceNotFoundException("Unable to read offer response for id: " + offerId);
        }
    }

    public WalletBalanceResponse fetchWalletBalance(Long customerId, String authorizationHeader) {
        log.info("CHECKING WALLET BALANCE customerId={}", customerId);
        String url = userServiceBaseUrl + walletBalancePath;
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(authorizationHeader)),
                    WalletBalanceResponse.class,
                    Map.of("userId", customerId, "customerId", customerId)
            ).getBody();
        } catch (RestClientException ex) {
            throw new PaymentException("Unable to verify wallet balance");
        }
    }

    public void deductWalletBalance(Long customerId, BigDecimal amount, String authorizationHeader) {
        String url = userServiceBaseUrl + walletDeductPath;
        HttpHeaders headers = authHeaders(authorizationHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            log.info("DEDUCTING WALLET BALANCE customerId={} amount={} url={}", customerId, amount, url);
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(new DeductWalletRequest(amount), headers),
                    Void.class,
                    Map.of("userId", customerId, "customerId", customerId)
            );
            log.info("PAYMENT SUCCESS customerId={} amount={}", customerId, amount);
        } catch (HttpClientErrorException.BadRequest ex) {
            log.info("PAYMENT REJECTED INSUFFICIENT BALANCE customerId={} amount={} response={}",
                    customerId,
                    amount,
                    ex.getResponseBodyAsString());
            throw new InsufficientBalanceException("Insufficient wallet balance");
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("PAYMENT CUSTOMER NOT FOUND customerId={} response={}", customerId, ex.getResponseBodyAsString());
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        } catch (HttpStatusCodeException ex) {
            log.warn("USER SERVICE WALLET DEDUCT ERROR customerId={} status={} body={}",
                    customerId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw new PaymentException("Unable to deduct wallet balance: user-service returned " + ex.getStatusCode());
        } catch (RestClientException ex) {
            log.warn("USER SERVICE WALLET DEDUCT CALL FAILED customerId={} message={}", customerId, ex.getMessage());
            throw new PaymentException("Unable to deduct wallet balance");
        }
    }

    public void refundWalletBalance(Long customerId, BigDecimal amount, String authorizationHeader) {
        String url = userServiceBaseUrl + walletRefundPath;
        HttpHeaders headers = authHeaders(authorizationHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            log.info("REFUNDING WALLET BALANCE customerId={} amount={} url={}", customerId, amount, url);
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(new DeductWalletRequest(amount), headers),
                    Void.class,
                    Map.of("userId", customerId, "customerId", customerId)
            );
            log.info("PAYMENT REFUND SUCCESS customerId={} amount={}", customerId, amount);
        } catch (RestClientException ex) {
            log.error("USER SERVICE WALLET REFUND FAILED customerId={} amount={} message={}",
                    customerId,
                    amount,
                    ex.getMessage());
            throw new PaymentException("Unable to refund wallet balance");
        }
    }

    private HttpHeaders authHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return headers;
    }

    private OfferResponse parseOffer(String payload, Long requestedOfferId) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Offer response body is empty");
        }

        JsonNode node = objectMapper.readTree(payload);
        return new OfferResponse(
                longValue(node, "id", requestedOfferId),
                longValue(node, "providerId", null),
                textValue(node, "title"),
                textValue(node, "description"),
                decimalValue(node, "price"),
                booleanValue(node, "available"),
                textValue(node, "category"),
                localDateTimeValue(node, "availableDateTime"),
                textValue(node, "availabilityStatus")
        );
    }

    private Long longValue(JsonNode node, String fieldName, Long fallback) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? fallback : field.longValue();
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }

    private BigDecimal decimalValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.decimalValue();
    }

    private Boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? null : field.booleanValue();
    }

    private LocalDateTime localDateTimeValue(JsonNode node, String fieldName) {
        String value = textValue(node, fieldName);
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}
