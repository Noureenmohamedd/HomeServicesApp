package org.example.notificationservice.notification;

import org.example.notificationservice.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishBookingConfirmation(NotificationMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.BOOKING_EXCHANGE,
                RabbitMqConfig.BOOKING_CONFIRMATION_ROUTING_KEY,
                message
        );
    }

    public void publishBookingFailure(NotificationMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.BOOKING_EXCHANGE,
                RabbitMqConfig.BOOKING_FAILURE_ROUTING_KEY,
                message
        );
    }
}
