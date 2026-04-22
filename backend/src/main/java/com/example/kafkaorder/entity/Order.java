package com.example.kafkaorder.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Entity — stored in H2 database.
 * Status lifecycle:
 *   PENDING → (Kafka Consumer picks up) → PROCESSING → COMPLETED | FAILED
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Order status — updated by Kafka Consumer after async processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column
    private String statusMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime processedAt;

    // ── Lifecycle ──
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
    }

    /**
     * Enum representing every stage an order goes through.
     * Kafka enables async status transitions without blocking the API.
     */
    public enum OrderStatus {
        PENDING,     // Order saved to DB, Kafka message published
        PROCESSING,  // Kafka Consumer received the message
        COMPLETED,   // Processing succeeded
        FAILED       // Processing failed (random 20% chance to demo error handling)
    }
}
