package com.example.bookingservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange bookingExchange(@Value("${booking.rabbitmq.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue bookingQueue(@Value("${booking.rabbitmq.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding bookingBinding(
            Queue bookingQueue,
            TopicExchange bookingExchange,
            @Value("${booking.rabbitmq.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(routingKey);
    }
}
