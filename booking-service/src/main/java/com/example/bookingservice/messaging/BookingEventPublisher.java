package com.example.bookingservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${booking.rabbitmq.routing-key}")
    private String routingKey;

    public void publishBookingEvent(BookingEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, toJson(event));
        log.info("RABBITMQ EVENT SENT eventType={} bookingId={} status={}",
                event.eventType(),
                event.bookingId(),
                event.status());
    }

    private String toJson(BookingEvent event) {
        return """
                {"eventType":"%s","bookingId":%d,"customerId":%d,"providerId":%d,"status":"%s","amount":%s}\
                """.formatted(
                event.eventType(),
                event.bookingId(),
                event.customerId(),
                event.providerId(),
                event.status(),
                event.amount()
        );
    }
}
