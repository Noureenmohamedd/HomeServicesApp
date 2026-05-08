package com.example.bookingservice.service;

import com.example.bookingservice.config.AuthenticatedUser;
import com.example.bookingservice.dto.AdminBookingResponse;
import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CompletedCustomerResponse;
import com.example.bookingservice.dto.CompletedProviderOfferResponse;
import com.example.bookingservice.dto.CreateBookingRequest;
import com.example.bookingservice.dto.OfferBookingSummaryResponse;
import com.example.bookingservice.dto.OfferResponse;
import com.example.bookingservice.dto.WalletBalanceResponse;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.BookingStatus;
import com.example.bookingservice.exception.InsufficientBalanceException;
import com.example.bookingservice.exception.InvalidBookingStateException;
import com.example.bookingservice.exception.PaymentException;
import com.example.bookingservice.exception.ResourceNotFoundException;
import com.example.bookingservice.exception.UnauthorizedActionException;
import com.example.bookingservice.messaging.BookingEvent;
import com.example.bookingservice.messaging.BookingEventPublisher;
import com.example.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ExternalServiceClient externalServiceClient;
    private final BookingEventPublisher bookingEventPublisher;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public BookingResponse createBooking(
            CreateBookingRequest request,
            AuthenticatedUser authenticatedUser,
            String authorizationHeader
    ) {
        log.info("BOOKING REQUEST RECEIVED customerId={} offerId={}", authenticatedUser.userId(), request.offerId());
        ensureRole(authenticatedUser, "CUSTOMER");

        OfferResponse offer = externalServiceClient.fetchOffer(request.offerId(), authorizationHeader);
        if (offer == null) {
            throw new ResourceNotFoundException("Offer not found: " + request.offerId());
        }
        BigDecimal amount = offer.price();
        if (!isOfferAvailableForBooking(offer)) {
            log.warn("BOOKING REJECTED OFFER UNAVAILABLE offerId={} availabilityStatus={} availableDateTime={} legacyAvailable={}",
                    offer.id(),
                    offer.availabilityStatus(),
                    offer.availableDateTime(),
                    offer.available());
            Booking rejectedBooking = saveBooking(authenticatedUser, offer, amount, BookingStatus.REJECTED, nowUtc());
            publishLifecycleEvent(rejectedBooking, "Booking rejected because the offer is unavailable");
            return toResponse(rejectedBooking);
        }

        LocalDateTime now = nowUtc();
        boolean walletDeducted = false;
        try {
            WalletBalanceResponse walletBalance = externalServiceClient.fetchWalletBalance(
                    authenticatedUser.userId(),
                    authorizationHeader
            );
            if (walletBalance == null || walletBalance.balance() == null
                    || walletBalance.balance().compareTo(amount) < 0) {
                Booking rejectedBooking = saveBooking(authenticatedUser, offer, amount, BookingStatus.REJECTED, now);
                publishLifecycleEvent(rejectedBooking, "Booking rejected because the wallet balance is insufficient");
                return toResponse(rejectedBooking);
            }

            externalServiceClient.deductWalletBalance(authenticatedUser.userId(), amount, authorizationHeader);
            walletDeducted = true;
            Booking confirmedBooking = saveBooking(authenticatedUser, offer, amount, BookingStatus.CONFIRMED, now);
            log.info("BOOKING CREATED bookingId={} status={} customerId={} providerId={}",
                    confirmedBooking.getId(),
                    confirmedBooking.getStatus(),
                    confirmedBooking.getCustomerId(),
                    confirmedBooking.getProviderId());
            publishLifecycleEvent(confirmedBooking, "Booking confirmed successfully");
            return toResponse(confirmedBooking);
        } catch (InsufficientBalanceException ex) {
            log.info("BOOKING TRANSITION REJECTED customerId={} offerId={} amount={} reason={}",
                    authenticatedUser.userId(),
                    offer.id(),
                    amount,
                    ex.getMessage());
            Booking rejectedBooking = saveBooking(authenticatedUser, offer, amount, BookingStatus.REJECTED, now);
            publishLifecycleEvent(rejectedBooking, "Booking rejected because the wallet balance is insufficient");
            return toResponse(rejectedBooking);
        } catch (PaymentException ex) {
            log.warn("BOOKING TRANSITION REJECTED PAYMENT ERROR customerId={} offerId={} amount={} reason={}",
                    authenticatedUser.userId(),
                    offer.id(),
                    amount,
                    ex.getMessage());
            Booking rejectedBooking = saveBooking(authenticatedUser, offer, amount, BookingStatus.REJECTED, now);
            publishLifecycleEvent(rejectedBooking, "Booking rejected because payment could not be processed");
            return toResponse(rejectedBooking);
        } catch (RuntimeException ex) {
            if (walletDeducted) {
                externalServiceClient.refundWalletBalance(authenticatedUser.userId(), amount, authorizationHeader);
            }
            throw ex;
        }
    }

    private Booking saveBooking(
            AuthenticatedUser authenticatedUser,
            OfferResponse offer,
            BigDecimal amount,
            BookingStatus status,
            LocalDateTime bookingDate
    ) {
        Booking booking = Booking.builder()
                .customerId(authenticatedUser.userId())
                .customerName(authenticatedUser.displayName())
                .providerId(offer.providerId())
                .offerId(offer.id())
                .serviceTitle(offer.title())
                .category(offer.category())
                .amount(amount)
                .bookingDate(bookingDate)
                .availableDateTime(offer.availableDateTime())
                .status(status)
                .build();

        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCustomerBookings(Long customerId, AuthenticatedUser authenticatedUser) {
        log.info("CUSTOMER BOOKING HISTORY REQUEST customerId={} authenticatedUserId={}",
                customerId,
                authenticatedUser == null ? null : authenticatedUser.userId());
        ensureRole(authenticatedUser, "CUSTOMER");
        ensureSameUser(customerId, authenticatedUser);
        List<BookingResponse> bookings = bookingRepository.findByCustomerIdOrderByBookingDateDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("CUSTOMER BOOKING HISTORY RETURNED customerId={} count={}", customerId, bookings.size());
        return bookings;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getProviderBookings(Long providerId, AuthenticatedUser authenticatedUser) {
        log.info("PROVIDER BOOKING VIEW REQUEST providerId={} authenticatedUserId={}",
                providerId,
                authenticatedUser == null ? null : authenticatedUser.userId());
        ensureRole(authenticatedUser, "PROVIDER");
        ensureSameUser(providerId, authenticatedUser);
        List<BookingResponse> bookings = bookingRepository.findByProviderIdOrderByBookingDateDesc(providerId)
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("PROVIDER BOOKING VIEW RETURNED providerId={} count={}", providerId, bookings.size());
        return bookings;
    }

    @Transactional(readOnly = true)
    public List<CompletedProviderOfferResponse> getCompletedProviderBookings(Long providerId, AuthenticatedUser authenticatedUser) {
        log.info("PROVIDER COMPLETED BOOKING VIEW REQUEST providerId={} authenticatedUserId={}",
                providerId,
                authenticatedUser == null ? null : authenticatedUser.userId());
        ensureProviderOwnerOrAdmin(providerId, authenticatedUser);
        List<CompletedProviderOfferResponse> bookings = groupCompletedBookingsByOffer(bookingRepository
                .findByProviderIdAndStatusOrderByBookingDateDesc(providerId, BookingStatus.COMPLETED)
        );
        log.info("PROVIDER COMPLETED BOOKING VIEW RETURNED providerId={} count={}", providerId, bookings.size());
        return bookings;
    }

    @Transactional(readOnly = true)
    public List<OfferBookingSummaryResponse> getOfferBookings(Long offerId, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new UnauthorizedActionException("Unauthorized user");
        }
        log.info("OFFER BOOKING SUMMARY REQUEST offerId={} authenticatedUserId={} role={}",
                offerId,
                authenticatedUser.userId(),
                authenticatedUser.role());
        return bookingRepository.findByOfferIdOrderByBookingDateDesc(offerId)
                .stream()
                .map(this::toOfferBookingSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookingHistory(AuthenticatedUser authenticatedUser) {
        ensureRole(authenticatedUser, "ADMIN");
        log.info("ADMIN BOOKING HISTORY REQUEST adminId={}", authenticatedUser.userId());
        List<AdminBookingResponse> bookings = bookingRepository.findAllByOrderByBookingDateDesc()
                .stream()
                .map(this::toAdminBookingResponse)
                .toList();
        log.info("ADMIN BOOKING HISTORY RETURNED adminId={} count={}", authenticatedUser.userId(), bookings.size());
        return bookings;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookingsForAdmin(AuthenticatedUser authenticatedUser) {
        ensureRole(authenticatedUser, "ADMIN");
        log.info("ADMIN ALL BOOKINGS REQUEST adminId={}", authenticatedUser.userId());
        List<BookingResponse> bookings = bookingRepository.findAllByOrderByBookingDateDesc()
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("ADMIN ALL BOOKINGS RETURNED adminId={} count={}", authenticatedUser.userId(), bookings.size());
        return bookings;
    }

    @Transactional
    public BookingResponse completeBooking(Long bookingId, AuthenticatedUser authenticatedUser) {
        log.info("BOOKING COMPLETION REQUEST bookingId={} authenticatedUserId={} role={}",
                bookingId,
                authenticatedUser == null ? null : authenticatedUser.userId(),
                authenticatedUser == null ? null : authenticatedUser.role());
        ensureRole(authenticatedUser, "PROVIDER");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getProviderId().equals(authenticatedUser.userId())) {
            log.warn("PROVIDER ACCESS DENIED bookingId={} bookingProviderId={} authenticatedProviderId={}",
                    bookingId,
                    booking.getProviderId(),
                    authenticatedUser.userId());
            throw new UnauthorizedActionException("Provider can only complete his own bookings");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("Only CONFIRMED bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);
        log.info("BOOKING COMPLETED bookingId={} providerId={}", savedBooking.getId(), savedBooking.getProviderId());
        publishLifecycleEvent(savedBooking, "Booking marked as completed");
        return toResponse(savedBooking);
    }

    private void ensureRole(AuthenticatedUser user, String expectedRole) {
        if (user == null || !expectedRole.equalsIgnoreCase(user.role())) {
            throw new UnauthorizedActionException("Unauthorized user");
        }
    }

    private void ensureSameUser(Long requestedUserId, AuthenticatedUser authenticatedUser) {
        if (!requestedUserId.equals(authenticatedUser.userId())) {
            throw new UnauthorizedActionException("Unauthorized user");
        }
    }

    private void ensureProviderOwnerOrAdmin(Long providerId, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new UnauthorizedActionException("Unauthorized user");
        }
        if ("ADMIN".equalsIgnoreCase(authenticatedUser.role())) {
            return;
        }
        if ("PROVIDER".equalsIgnoreCase(authenticatedUser.role()) && providerId.equals(authenticatedUser.userId())) {
            return;
        }
        throw new UnauthorizedActionException("Unauthorized user");
    }

    private boolean isOfferAvailableForBooking(OfferResponse offer) {
        if (offer == null || offer.id() == null) {
            return false;
        }
        if (isManuallyAvailable(offer)) {
            log.info("OFFER AVAILABLE BY MANUAL STATUS offerId={} availabilityStatus={}",
                    offer.id(),
                    offer.availabilityStatus());
            return true;
        }
        if ("FUTURE_AVAILABLE".equalsIgnoreCase(trimmedStatus(offer))) {
            boolean bookableByDate = offer.availableDateTime() != null && !offer.availableDateTime().isBefore(nowUtc());
            log.info("OFFER FUTURE AVAILABILITY CHECK offerId={} availableDateTime={} bookableByDate={}",
                    offer.id(),
                    offer.availableDateTime(),
                    bookableByDate);
            return bookableByDate;
        }
        if (offer.availabilityStatus() != null && !offer.availabilityStatus().isBlank()) {
            log.info("OFFER NOT BOOKABLE BY STATUS offerId={} availabilityStatus={}",
                    offer.id(),
                    offer.availabilityStatus());
            return false;
        }
        boolean bookableByDate = offer.availableDateTime() != null && !offer.availableDateTime().isBefore(nowUtc());
        log.info("OFFER DATE AVAILABILITY CHECK offerId={} availableDateTime={} bookableByDate={}",
                offer.id(),
                offer.availableDateTime(),
                bookableByDate);
        return bookableByDate;
    }

    private boolean isManuallyAvailable(OfferResponse offer) {
        return "AVAILABLE".equalsIgnoreCase(trimmedStatus(offer));
    }

    private String trimmedStatus(OfferResponse offer) {
        return offer.availabilityStatus() == null ? null : offer.availabilityStatus().trim();
    }

    private void publishLifecycleEvent(Booking booking, String message) {
        bookingEventPublisher.publishBookingEvent(new BookingEvent(
                eventTypeFor(booking.getStatus()),
                booking.getId(),
                booking.getCustomerId(),
                booking.getProviderId(),
                booking.getOfferId(),
                booking.getServiceTitle(),
                booking.getCategory(),
                publicStatus(booking.getStatus()),
                booking.getAmount(),
                message
        ));
    }

    private String eventTypeFor(BookingStatus status) {
        return "BOOKING_" + publicStatus(status).name();
    }

    private String statusMeaning(Booking booking) {
        boolean expired = booking.getAvailableDateTime() != null && !nowUtc().isBefore(booking.getAvailableDateTime());
        BookingStatus status = publicStatus(booking.getStatus());
        if ((status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED) && !expired) {
            return "CURRENT";
        }
        return "PAST";
    }

    private BookingStatus publicStatus(BookingStatus status) {
        return status == BookingStatus.FAILED ? BookingStatus.REJECTED : status;
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(clock);
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getCustomerName(),
                booking.getOfferId(),
                booking.getProviderId(),
                booking.getServiceTitle(),
                booking.getCategory(),
                booking.getAmount(),
                publicStatus(booking.getStatus()),
                statusMeaning(booking),
                booking.getBookingDate(),
                booking.getAvailableDateTime()
        );
    }

    private List<CompletedProviderOfferResponse> groupCompletedBookingsByOffer(List<Booking> completedBookings) {
        Map<Long, List<Booking>> bookingsByOffer = new LinkedHashMap<>();
        completedBookings.forEach(booking -> bookingsByOffer
                .computeIfAbsent(booking.getOfferId(), ignored -> new java.util.ArrayList<>())
                .add(booking));

        return bookingsByOffer.values()
                .stream()
                .map(this::toCompletedProviderOfferResponse)
                .toList();
    }

    private CompletedProviderOfferResponse toCompletedProviderOfferResponse(List<Booking> offerBookings) {
        Booking firstBooking = offerBookings.get(0);
        return new CompletedProviderOfferResponse(
                firstBooking.getOfferId(),
                firstBooking.getServiceTitle(),
                firstBooking.getCategory(),
                firstBooking.getAmount(),
                firstBooking.getAvailableDateTime(),
                offerBookings.stream()
                        .map(this::toCompletedCustomerResponse)
                        .toList()
        );
    }

    private CompletedCustomerResponse toCompletedCustomerResponse(Booking booking) {
        return new CompletedCustomerResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getCustomerName(),
                booking.getAmount(),
                booking.getBookingDate()
        );
    }

    private OfferBookingSummaryResponse toOfferBookingSummaryResponse(Booking booking) {
        return new OfferBookingSummaryResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getCustomerName(),
                publicStatus(booking.getStatus()),
                booking.getAmount(),
                booking.getBookingDate()
        );
    }

    private AdminBookingResponse toAdminBookingResponse(Booking booking) {
        return new AdminBookingResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getCustomerName(),
                booking.getProviderId(),
                booking.getOfferId(),
                booking.getServiceTitle(),
                booking.getCategory(),
                booking.getAmount(),
                publicStatus(booking.getStatus()),
                booking.getBookingDate(),
                booking.getAvailableDateTime()
        );
    }
}
