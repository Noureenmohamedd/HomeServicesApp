package com.example.offerservice.repository;

import com.example.offerservice.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByProviderId(Long providerId);

    List<Offer> findByProviderIdOrderByAvailableDateTimeAsc(Long providerId);

    @Query("""
            select offer
            from Offer offer
            where offer.availableDateTime > :now
              and upper(offer.availabilityStatus) = 'AVAILABLE'
            order by offer.availableDateTime asc
            """)
    List<Offer> findActiveOffers(@Param("now") LocalDateTime now);

    @Query("""
            select offer
            from Offer offer
            where offer.providerId = :providerId
              and offer.availableDateTime > :now
              and upper(offer.availabilityStatus) = 'AVAILABLE'
            order by offer.availableDateTime asc
            """)
    List<Offer> findActiveOffersByProviderId(
            @Param("providerId") Long providerId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select offer
            from Offer offer
            where lower(offer.category) = lower(:category)
              and upper(offer.availabilityStatus) = 'AVAILABLE'
              and offer.availableDateTime >= :now
            order by offer.availableDateTime asc
            """)
    List<Offer> findBookableOffersByCategory(
            @Param("category") String category,
            @Param("now") LocalDateTime now
    );
}
