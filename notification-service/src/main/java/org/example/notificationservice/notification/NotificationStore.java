package org.example.notificationservice.notification;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class NotificationStore {

    private final ConcurrentMap<Long, List<NotificationResponse>> customerNotifications = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, List<NotificationResponse>> providerNotifications = new ConcurrentHashMap<>();

    public NotificationResponse save(NotificationMessage message, RecipientType recipientType, Long recipientId) {
        NotificationResponse notification = new NotificationResponse(
                UUID.randomUUID(),
                message.type(),
                message.message(),
                message.customerId(),
                message.serviceProviderId(),
                message.bookingId(),
                message.serviceName(),
                message.amount(),
                message.status(),
                recipientType,
                recipientId,
                Instant.now()
        );

        ConcurrentMap<Long, List<NotificationResponse>> targetStore = recipientType == RecipientType.CUSTOMER
                ? customerNotifications
                : providerNotifications;
        targetStore.compute(recipientId, (id, notifications) -> {
            List<NotificationResponse> updated = notifications == null ? new ArrayList<>() : new ArrayList<>(notifications);
            updated.add(notification);
            updated.sort(Comparator.comparing(NotificationResponse::createdAt).reversed());
            return updated;
        });
        return notification;
    }

    public List<NotificationResponse> findByCustomerId(Long customerId) {
        return List.copyOf(customerNotifications.getOrDefault(customerId, List.of()));
    }

    public List<NotificationResponse> findByProviderId(Long providerId) {
        return List.copyOf(providerNotifications.getOrDefault(providerId, List.of()));
    }
}
