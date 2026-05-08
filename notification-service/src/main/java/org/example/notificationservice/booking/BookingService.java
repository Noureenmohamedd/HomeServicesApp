package org.example.notificationservice.booking;

import org.example.notificationservice.notification.NotificationMessage;
import org.example.notificationservice.notification.NotificationPublisher;
import org.example.notificationservice.wallet.WalletEJB;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingService {

    private final AtomicLong bookingSequence = new AtomicLong(1);
    private final WalletEJB walletEJB;
    private final NotificationPublisher notificationPublisher;

    public BookingService(WalletEJB walletEJB, NotificationPublisher notificationPublisher) {
        this.walletEJB = walletEJB;
        this.notificationPublisher = notificationPublisher;
    }

    public BookingResponse book(BookingRequest request) {
        Long bookingId = bookingSequence.getAndIncrement();

        if (!walletEJB.hasSufficientBalance(request.customerId(), request.price())) {
            NotificationMessage failure = new NotificationMessage(
                    "BOOKING_REJECTED",
                    bookingId,
                    request.customerId(),
                    request.serviceProviderId(),
                    null,
                    request.serviceName(),
                    null,
                    "REJECTED",
                    request.price(),
                    "Your booking was rejected because your wallet balance is insufficient."
            );
            notificationPublisher.publishBookingFailure(failure);
            return new BookingResponse(
                    bookingId,
                    request.customerId(),
                    request.serviceProviderId(),
                    request.serviceName(),
                    request.price(),
                    walletEJB.getBalance(request.customerId()),
                    BookingStatus.REJECTED,
                    "Booking rejected: insufficient wallet balance."
            );
        }

        BigDecimal remainingBalance = walletEJB.deduct(request.customerId(), request.price());
        try {
            NotificationMessage confirmation = new NotificationMessage(
                    "BOOKING_CONFIRMED",
                    bookingId,
                    request.customerId(),
                    request.serviceProviderId(),
                    null,
                    request.serviceName(),
                    null,
                    "CONFIRMED",
                    request.price(),
                    "Your booking has been confirmed with the " + request.serviceName() + "."
            );
            notificationPublisher.publishBookingConfirmation(confirmation);
            return new BookingResponse(
                    bookingId,
                    request.customerId(),
                    request.serviceProviderId(),
                    request.serviceName(),
                    request.price(),
                    remainingBalance,
                    BookingStatus.CONFIRMED,
                    "Booking confirmed and notification queued."
            );
        } catch (RuntimeException ex) {
            BigDecimal restoredBalance = walletEJB.refund(request.customerId(), request.price());
            NotificationMessage failure = new NotificationMessage(
                    "BOOKING_REJECTED",
                    bookingId,
                    request.customerId(),
                    request.serviceProviderId(),
                    null,
                    request.serviceName(),
                    null,
                    "REJECTED",
                    request.price(),
                    "Your booking failed and the deducted amount was returned to your wallet."
            );
            notificationPublisher.publishBookingFailure(failure);
            return new BookingResponse(
                    bookingId,
                    request.customerId(),
                    request.serviceProviderId(),
                    request.serviceName(),
                    request.price(),
                    restoredBalance,
                    BookingStatus.REJECTED,
                    "Booking failed; wallet deduction was rolled back."
            );
        }
    }
}
