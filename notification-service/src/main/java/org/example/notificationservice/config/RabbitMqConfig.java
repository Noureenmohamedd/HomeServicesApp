package org.example.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String BOOKING_EXCHANGE = "booking.notifications.exchange";
    public static final String BOOKING_CONFIRMATION_QUEUE = "booking.confirmation.notifications.queue";
    public static final String BOOKING_FAILURE_QUEUE = "booking.failure.notifications.queue";
    public static final String BOOKING_CONFIRMATION_ROUTING_KEY = "booking.confirmed";
    public static final String BOOKING_FAILURE_ROUTING_KEY = "booking.failed";

    @Bean
    DirectExchange bookingNotificationsExchange() {
        return new DirectExchange(BOOKING_EXCHANGE);
    }

    @Bean
    Queue bookingConfirmationQueue() {
        return new Queue(BOOKING_CONFIRMATION_QUEUE, true);
    }

    @Bean
    Queue bookingFailureQueue() {
        return new Queue(BOOKING_FAILURE_QUEUE, true);
    }

    @Bean
    Binding bookingConfirmationBinding(Queue bookingConfirmationQueue, DirectExchange bookingNotificationsExchange) {
        return BindingBuilder.bind(bookingConfirmationQueue)
                .to(bookingNotificationsExchange)
                .with(BOOKING_CONFIRMATION_ROUTING_KEY);
    }

    @Bean
    Binding bookingFailureBinding(Queue bookingFailureQueue, DirectExchange bookingNotificationsExchange) {
        return BindingBuilder.bind(bookingFailureQueue)
                .to(bookingNotificationsExchange)
                .with(BOOKING_FAILURE_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
