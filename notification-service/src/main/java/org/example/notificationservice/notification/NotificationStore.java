package org.example.notificationservice.notification;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class NotificationStore {

    private final NotificationRepository notificationRepository;

    public NotificationStore(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse save(NotificationMessage message, RecipientType recipientType, Long recipientId) {
        NotificationEntity notification = new NotificationEntity(
                message.type(),
                message.message(),
                message.customerId(),
                message.serviceProviderId(),
                message.bookingId(),
                message.serviceName(),
                message.amount(),
                message.status(),
                recipientType,
                recipientId
        );

        return notificationRepository.save(notification).toResponse();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByCustomerId(Long customerId) {
        return findByRecipient(RecipientType.CUSTOMER, customerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findByProviderId(Long providerId) {
        return findByRecipient(RecipientType.SERVICE_PROVIDER, providerId);
    }

    private List<NotificationResponse> findByRecipient(RecipientType recipientType, Long recipientId) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId)
                .stream()
                .map(NotificationEntity::toResponse)
                .toList();
    }
}
