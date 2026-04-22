package com.example.kafkaorder.service;

import com.example.kafkaorder.controller.OrderController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bridges the Kafka Consumer → SSE Controller.
 *
 * When the Kafka Consumer updates an order status, it fires a Spring
 * ApplicationEvent. This listener picks it up and tells the
 * OrderController to push SSE updates to all connected Angular clients.
 *
 * Why events? Avoids circular dependency between Consumer ↔ Controller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusEventListener {

    private final OrderController orderController;

    @EventListener
    @Async
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("📢 Broadcasting SSE update for orderId={}", event.getOrderId());
        orderController.notifyClients();
    }
}
