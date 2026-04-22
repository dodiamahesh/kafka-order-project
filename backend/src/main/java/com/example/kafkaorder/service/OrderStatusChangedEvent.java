package com.example.kafkaorder.service;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent fired when a Kafka Consumer updates an order's status.
 * This triggers SSE notifications to all connected Angular clients.
 */
@Getter
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Long orderId;
    private final String newStatus;

    public OrderStatusChangedEvent(Object source, Long orderId, String newStatus) {
        super(source);
        this.orderId = orderId;
        this.newStatus = newStatus;
    }
}
