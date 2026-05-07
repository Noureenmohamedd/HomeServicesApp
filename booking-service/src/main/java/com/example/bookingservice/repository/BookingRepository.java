package com.example.bookingservice.repository;

import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByBookingDateDesc(Long customerId);

    List<Booking> findByProviderIdOrderByBookingDateDesc(Long providerId);

    List<Booking> findByProviderIdAndStatusOrderByBookingDateDesc(Long providerId, BookingStatus status);

    List<Booking> findByOfferIdOrderByBookingDateDesc(Long offerId);

    List<Booking> findAllByOrderByBookingDateDesc();
}
