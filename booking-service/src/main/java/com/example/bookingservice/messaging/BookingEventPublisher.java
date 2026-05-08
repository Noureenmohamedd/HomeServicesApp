package com.example.bookingservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${booking.rabbitmq.exchange}")
    private String exchange;

    @Value("${booking.rabbitmq.routing-key.confirmed}")
    private String confirmedRoutingKey;

    @Value("${booking.rabbitmq.routing-key.rejected}")
    private String rejectedRoutingKey;

    @Value("${booking.rabbitmq.routing-key.completed}")
    private String completedRoutingKey;

    public void publishBookingEvent(BookingEvent event) {
        String routingKey = routingKeyFor(event.status());
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("RABBITMQ EVENT SENT eventType={} bookingId={} status={} routingKey={}",
                    event.eventType(),
                    event.bookingId(),
                    event.status(),
                    routingKey);
        } catch (AmqpException exception) {
            log.error("RABBITMQ EVENT FAILED eventType={} bookingId={} status={} routingKey={} message={}",
                    event.eventType(),
                    event.bookingId(),
                    event.status(),
                    routingKey,
                    exception.getMessage());
        }
    }

    private String routingKeyFor(com.example.bookingservice.entity.BookingStatus status) {
        return switch (status) {
            case CONFIRMED -> confirmedRoutingKey;
            case COMPLETED -> completedRoutingKey;
            case REJECTED, FAILED -> rejectedRoutingKey;
            case PENDING -> confirmedRoutingKey;
        };
    }
}
