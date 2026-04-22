package com.example.kafkaorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderEvent — the message published to Kafka topics.
 *
 * WHY a separate DTO and not the entity?
 * ──────────────────────────────────────
 * Kafka messages should be decoupled from your DB schema.
 * This DTO is serialized to JSON and sent over the wire.
 * If your entity changes, Kafka consumers won't break
 * as long as this contract stays stable.
 *
 * This class is used for BOTH topics:
 *  → order-placed   (produced by OrderProducer)
 *  → order-processed (produced after consumer finishes)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private Long orderId;
    private String customerName;
    private String product;
    private Integer quantity;
    private BigDecimal price;
    private String status;          // PENDING | PROCESSING | COMPLETED | FAILED
    private String statusMessage;
    private LocalDateTime eventTime;

    // Factory method for convenience
    public static OrderEvent of(Long orderId, String customerName, String product,
                                 Integer quantity, BigDecimal price, String status, String message) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setCustomerName(customerName);
        event.setProduct(product);
        event.setQuantity(quantity);
        event.setPrice(price);
        event.setStatus(status);
        event.setStatusMessage(message);
        event.setEventTime(LocalDateTime.now());
        return event;
    }
}
