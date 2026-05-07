package com.example.offerservice.service;

import com.example.offerservice.dto.ActiveOfferResponse;
import com.example.offerservice.dto.AvailabilityStatus;
import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.entity.Offer;
import com.example.offerservice.exception.ResourceNotFoundException;
import com.example.offerservice.repository.OfferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OfferService {

    private static final String PROFESSION_CATEGORY_ERROR =
            "Provider cannot create offers outside their profession category";
    private static final String PAST_AVAILABILITY_ERROR =
            "Available date and time cannot be in the past";
    private static final String EMPTY_UPDATE_ERROR =
            "At least one of price, availableDateTime, or availabilityStatus is required";

    private final OfferRepository offerRepository;
    private final ExternalServiceClient externalServiceClient;
    private final ServiceCategoryService serviceCategoryService;
    private final Clock clock;

    public OfferService(
            OfferRepository offerRepository,
            ExternalServiceClient externalServiceClient,
            ServiceCategoryService serviceCategoryService
    ) {
        this.offerRepository = offerRepository;
        this.externalServiceClient = externalServiceClient;
        this.serviceCategoryService = serviceCategoryService;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public OfferResponse createOffer(
            CreateOfferRequest request,
            Long providerId,
            String providerUsername,
            String professionType
    ) {
        validateProfessionCanCreateCategory(professionType, request.getCategory());
        String availabilityStatus = normalizeAvailabilityStatus(request.getAvailabilityStatus());
        validateAvailableDateTime(request.getAvailableDateTime(), availabilityStatus);

        Offer offer = Offer.builder()
                .providerId(providerId)
                .providerUsername(providerUsername)
                .providerProfessionType(normalizeProfessionType(professionType))
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(normalizeCategory(request.getCategory()))
                .availableDateTime(request.getAvailableDateTime())
                .availabilityStatus(availabilityStatus)
                .available(isAvailableStatus(availabilityStatus))
                .build();

        return mapToResponse(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getAllOffers() {
        return offerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOfferById(Long id) {
        return mapToResponse(findOfferById(id));
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffersByProviderId(Long providerId, Long authenticatedProviderId, String role) {
        ensureProviderCanAccessProviderOffers(providerId, authenticatedProviderId, role);
        return offerRepository.findByProviderIdOrderByAvailableDateTimeAsc(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffersByCategory(String category) {
        serviceCategoryService.getActiveCategoryOrThrow(category);
        return offerRepository.findBookableOffersByCategory(
                        normalizeCategory(category),
                        nowUtc()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActiveOfferResponse> getActiveOffers(Long authenticatedUserId, String role, String authorizationHeader) {
        List<Offer> offers = "PROVIDER".equalsIgnoreCase(role)
                ? offerRepository.findActiveOffersByProviderId(authenticatedUserId, nowUtc())
                : offerRepository.findActiveOffers(nowUtc());

        return offers
                .stream()
                .map(offer -> mapToActiveOfferResponse(offer, authorizationHeader))
                .toList();
    }

    @Transactional
    public OfferResponse updateOffer(Long id, UpdateOfferRequest request, Long providerId) {
        Offer offer = findOfferById(id);
        if (!offer.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider can only update his own offers");
        }

        boolean hasPrice = request.getPrice() != null;
        boolean hasAvailableDateTime = request.getAvailableDateTime() != null;
        boolean hasAvailabilityStatus = request.getAvailabilityStatus() != null
                && !request.getAvailabilityStatus().isBlank();

        if (!hasPrice && !hasAvailableDateTime && !hasAvailabilityStatus) {
            throw new IllegalArgumentException(EMPTY_UPDATE_ERROR);
        }

        String availabilityStatus = hasAvailabilityStatus
                ? normalizeAvailabilityStatus(request.getAvailabilityStatus())
                : offer.getAvailabilityStatus();

        if (hasPrice) {
            offer.setPrice(request.getPrice());
        }
        if (hasAvailableDateTime) {
            validateAvailableDateTime(request.getAvailableDateTime(), availabilityStatus);
            offer.setAvailableDateTime(request.getAvailableDateTime());
        }
        if (hasAvailabilityStatus) {
            validateAvailableDateTime(offer.getAvailableDateTime(), availabilityStatus);
            offer.setAvailabilityStatus(availabilityStatus);
            offer.setAvailable(isAvailableStatus(availabilityStatus));
        }

        return mapToResponse(offerRepository.save(offer));
    }

    @Transactional
    public void deleteOffer(Long id, Long providerId) {
        Offer offer = findOfferById(id);
        if (!offer.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider can only delete his own offers");
        }
        offerRepository.delete(offer);
    }

    @Transactional(readOnly = true)
    public boolean isOfferAvailableForBooking(Long offerId) {
        Offer offer = findOfferById(offerId);
        return isManuallyAvailable(offer) || isAutomaticallyAvailable(offer);
    }

    private Offer findOfferById(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
    }

    private OfferResponse mapToResponse(Offer offer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .providerId(offer.getProviderId())
                .providerUsername(offer.getProviderUsername())
                .providerProfessionType(offer.getProviderProfessionType())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .price(offer.getPrice())
                .available(offer.isAvailable())
                .category(offer.getCategory())
                .availableDateTime(offer.getAvailableDateTime())
                .availabilityStatus(getAvailabilityStatus(offer))
                .build();
    }

    private ActiveOfferResponse mapToActiveOfferResponse(Offer offer, String authorizationHeader) {
        String providerUsername = offer.getProviderUsername();
        String providerProfessionType = offer.getProviderProfessionType();

        if (providerUsername == null || providerUsername.isBlank()
                || providerProfessionType == null || providerProfessionType.isBlank()) {
            var providerProfile = externalServiceClient.fetchProviderProfile(offer.getProviderId(), authorizationHeader);
            if (providerProfile != null) {
                providerUsername = providerProfile.username();
                providerProfessionType = providerProfile.professionType();
            }
        }

        return ActiveOfferResponse.builder()
                .id(offer.getId())
                .offerId(offer.getId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .category(offer.getCategory())
                .price(offer.getPrice())
                .available(offer.isAvailable())
                .availabilityStatus(getAvailabilityStatus(offer))
                .availableDateTime(offer.getAvailableDateTime())
                .providerId(offer.getProviderId())
                .providerUsername(providerUsername)
                .providerProfessionType(providerProfessionType)
                .bookings(externalServiceClient.fetchBookingsForOffer(offer.getId(), authorizationHeader))
                .build();
    }

    private String normalizeCategory(String category) {
        return category == null ? null : category.trim();
    }

    private String normalizeProfessionType(String professionType) {
        return professionType == null ? null : professionType.trim().toUpperCase(Locale.ROOT);
    }

    private void validateProfessionCanCreateCategory(String professionType, String category) {
        if (professionType == null || professionType.isBlank() || category == null || category.isBlank()) {
            throw new IllegalArgumentException(PROFESSION_CATEGORY_ERROR);
        }

        if (!serviceCategoryService.professionCanUseCategory(professionType, category)) {
            throw new IllegalArgumentException(PROFESSION_CATEGORY_ERROR);
        }
    }

    private void ensureProviderCanAccessProviderOffers(Long providerId, Long authenticatedProviderId, String role) {
        if (!"PROVIDER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only providers can view provider offers");
        }
        if (!providerId.equals(authenticatedProviderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Provider can only view his own offers");
        }
    }

    private String getAvailabilityStatus(Offer offer) {
        if (!offer.isAvailable()) {
            return AvailabilityStatus.UNAVAILABLE.name();
        }
        if (offer.getAvailabilityStatus() != null && !offer.getAvailabilityStatus().isBlank()) {
            return offer.getAvailabilityStatus();
        }

        LocalDateTime availableDateTime = offer.getAvailableDateTime();
        if (availableDateTime != null && !availableDateTime.isBefore(nowUtc())) {
            return AvailabilityStatus.FUTURE_AVAILABLE.name();
        }
        return AvailabilityStatus.EXPIRED.name();
    }

    private String normalizeAvailabilityStatus(String availabilityStatus) {
        if (availabilityStatus == null || availabilityStatus.isBlank()) {
            return null;
        }

        String normalizedStatus = availabilityStatus.trim().toUpperCase(Locale.ROOT);
        try {
            return AvailabilityStatus.valueOf(normalizedStatus).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid availability status: " + availabilityStatus);
        }
    }

    private void validateAvailableDateTime(LocalDateTime availableDateTime, String availabilityStatus) {
        if (AvailabilityStatus.AVAILABLE.name().equals(availabilityStatus)
                || AvailabilityStatus.UNAVAILABLE.name().equals(availabilityStatus)) {
            return;
        }
        if (availableDateTime != null && availableDateTime.isBefore(nowUtc())) {
            throw new IllegalArgumentException(PAST_AVAILABILITY_ERROR);
        }
    }

    private boolean isManuallyAvailable(Offer offer) {
        return offer.isAvailable()
                && AvailabilityStatus.AVAILABLE.name().equalsIgnoreCase(offer.getAvailabilityStatus());
    }

    private boolean isAutomaticallyAvailable(Offer offer) {
        return offer.isAvailable()
                && offer.getAvailabilityStatus() == null
                && offer.getAvailableDateTime() != null
                && !offer.getAvailableDateTime().isBefore(nowUtc());
    }

    private boolean isAvailableStatus(String availabilityStatus) {
        if (availabilityStatus == null || availabilityStatus.isBlank()) {
            return true;
        }
        return AvailabilityStatus.AVAILABLE.name().equalsIgnoreCase(availabilityStatus)
                || AvailabilityStatus.FUTURE_AVAILABLE.name().equalsIgnoreCase(availabilityStatus);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(clock);
    }
}
