package org.example.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_CONFIRMATION_QUEUE = "booking.confirmed.notifications.queue";
    public static final String BOOKING_REJECTION_QUEUE = "booking.rejected.notifications.queue";
    public static final String BOOKING_COMPLETION_QUEUE = "booking.completed.notifications.queue";
    public static final String BOOKING_CONFIRMATION_ROUTING_KEY = "booking.confirmed";
    public static final String BOOKING_REJECTION_ROUTING_KEY = "booking.rejected";
    public static final String BOOKING_COMPLETION_ROUTING_KEY = "booking.completed";

    @Bean
    TopicExchange bookingNotificationsExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    Queue bookingConfirmationQueue() {
        return new Queue(BOOKING_CONFIRMATION_QUEUE, true);
    }

    @Bean
    Queue bookingRejectionQueue() {
        return new Queue(BOOKING_REJECTION_QUEUE, true);
    }

    @Bean
    Queue bookingCompletionQueue() {
        return new Queue(BOOKING_COMPLETION_QUEUE, true);
    }

    @Bean
    Binding bookingConfirmationBinding(Queue bookingConfirmationQueue, TopicExchange bookingNotificationsExchange) {
        return BindingBuilder.bind(bookingConfirmationQueue)
                .to(bookingNotificationsExchange)
                .with(BOOKING_CONFIRMATION_ROUTING_KEY);
    }

    @Bean
    Binding bookingRejectionBinding(Queue bookingRejectionQueue, TopicExchange bookingNotificationsExchange) {
        return BindingBuilder.bind(bookingRejectionQueue)
                .to(bookingNotificationsExchange)
                .with(BOOKING_REJECTION_ROUTING_KEY);
    }

    @Bean
    Binding bookingCompletionBinding(Queue bookingCompletionQueue, TopicExchange bookingNotificationsExchange) {
        return BindingBuilder.bind(bookingCompletionQueue)
                .to(bookingNotificationsExchange)
                .with(BOOKING_COMPLETION_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
