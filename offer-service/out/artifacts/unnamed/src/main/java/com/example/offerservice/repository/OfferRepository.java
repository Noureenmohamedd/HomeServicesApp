package com.example.offerservice.repository;

import com.example.offerservice.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByProviderId(Long providerId);
}
