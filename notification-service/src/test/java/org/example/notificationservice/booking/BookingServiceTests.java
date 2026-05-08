package org.example.notificationservice.booking;

import org.example.notificationservice.notification.NotificationMessage;
import org.example.notificationservice.notification.NotificationPublisher;
import org.example.notificationservice.notification.NotificationType;
import org.example.notificationservice.wallet.WalletEJB;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BookingServiceTests {

    private final WalletEJB walletEJB = new WalletEJB();
    private final NotificationPublisher notificationPublisher = mock(NotificationPublisher.class);
    private final BookingService bookingService = new BookingService(walletEJB, notificationPublisher);

    @Test
    void confirmedBookingDeductsWalletAndPublishesConfirmation() {
        walletEJB.addFunds(1L, BigDecimal.valueOf(200));

        BookingResponse response = bookingService.book(new BookingRequest(
                1L,
                2L,
                "plumber",
                BigDecimal.valueOf(80)
        ));

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.remainingWalletBalance()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(walletEJB.getBalance(1L)).isEqualByComparingTo(BigDecimal.valueOf(120));
        verify(notificationPublisher).publishBookingConfirmation(argThat(this::isConfirmationForExampleBooking));
        verify(notificationPublisher, never()).publishBookingFailure(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectedBookingKeepsWalletBalanceAndPublishesFailure() {
        walletEJB.addFunds(1L, BigDecimal.valueOf(50));

        BookingResponse response = bookingService.book(new BookingRequest(
                1L,
                2L,
                "plumber",
                BigDecimal.valueOf(80)
        ));

        assertThat(response.status()).isEqualTo(BookingStatus.REJECTED);
        assertThat(response.remainingWalletBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(walletEJB.getBalance(1L)).isEqualByComparingTo(BigDecimal.valueOf(50));
        verify(notificationPublisher).publishBookingFailure(argThat(message ->
                message.type() == NotificationType.BOOKING_REJECTION
                        && message.customerId().equals(1L)
                        && message.status().equals("REJECTED")
        ));
        verify(notificationPublisher, never()).publishBookingConfirmation(org.mockito.ArgumentMatchers.any());
    }

    private boolean isConfirmationForExampleBooking(NotificationMessage message) {
        return message.type() == NotificationType.BOOKING_CONFIRMATION
                && message.customerId().equals(1L)
                && message.serviceProviderId().equals(2L)
                && message.serviceName().equals("plumber")
                && message.amount().compareTo(BigDecimal.valueOf(80)) == 0
                && message.status().equals("CONFIRMED");
    }
}
