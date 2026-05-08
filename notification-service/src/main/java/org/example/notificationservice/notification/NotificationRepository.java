package org.example.notificationservice.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            RecipientType recipientType,
            Long recipientId
    );
}
