package com.example.offerservice.service;

import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.entity.Offer;
import com.example.offerservice.exception.ResourceNotFoundException;
import com.example.offerservice.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request, Long providerId) {
        Offer offer = Offer.builder()
                .providerId(providerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .available(request.getAvailable())
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
    public List<OfferResponse> getOffersByProviderId(Long providerId) {
        return offerRepository.findByProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OfferResponse updateOffer(Long id, UpdateOfferRequest request, Long providerId) {
        Offer offer = findOfferById(id);

        offer.setProviderId(providerId);
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setPrice(request.getPrice());
        offer.setAvailable(request.getAvailable());

        return mapToResponse(offerRepository.save(offer));
    }

    @Transactional
    public void deleteOffer(Long id) {
        Offer offer = findOfferById(id);
        offerRepository.delete(offer);
    }

    private Offer findOfferById(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
    }

    private OfferResponse mapToResponse(Offer offer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .providerId(offer.getProviderId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .price(offer.getPrice())
                .available(offer.isAvailable())
                .build();
    }
}
