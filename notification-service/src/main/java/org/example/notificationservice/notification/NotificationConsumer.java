package org.example.notificationservice.notification;

import org.example.notificationservice.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private final NotificationStore notificationStore;

    public NotificationConsumer(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @RabbitListener(queues = RabbitMqConfig.BOOKING_CONFIRMATION_QUEUE)
    public void consumeBookingConfirmation(NotificationMessage message) {
        notificationStore.save(message, RecipientType.CUSTOMER, message.customerId());
        notificationStore.save(message, RecipientType.SERVICE_PROVIDER, message.serviceProviderId());
    }

    @RabbitListener(queues = RabbitMqConfig.BOOKING_REJECTION_QUEUE)
    public void consumeBookingRejection(NotificationMessage message) {
        notificationStore.save(message, RecipientType.CUSTOMER, message.customerId());
    }

    @RabbitListener(queues = RabbitMqConfig.BOOKING_COMPLETION_QUEUE)
    public void consumeBookingCompletion(NotificationMessage message) {
        notificationStore.save(message, RecipientType.CUSTOMER, message.customerId());
        notificationStore.save(message, RecipientType.SERVICE_PROVIDER, message.serviceProviderId());
    }
}
