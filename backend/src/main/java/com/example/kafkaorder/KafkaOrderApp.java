package com.example.kafkaorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Kafka Order Processing Application.
 *
 * Architecture Overview:
 * ┌─────────────┐    HTTP POST     ┌──────────────────┐
 * │  Angular UI │ ──────────────▶  │  OrderController │
 * └─────────────┘                  └────────┬─────────┘
 *                                           │ saves Order (PENDING)
 *                                           ▼
 *                                    ┌─────────────┐
 *                                    │  H2 Database│
 *                                    └─────────────┘
 *                                           │ publishes event
 *                                           ▼
 *                                  ┌─────────────────┐
 *                                  │  OrderProducer  │──▶ Kafka Topic: order-placed
 *                                  └─────────────────┘
 *                                                              │
 *                                                     Kafka delivers message
 *                                                              │
 *                                                              ▼
 *                                                   ┌──────────────────┐
 *                                                   │  OrderConsumer   │ (async processing)
 *                                                   └────────┬─────────┘
 *                                                            │ updates status → COMPLETED/FAILED
 *                                                            ▼
 *                                                    ┌─────────────┐
 *                                                    │  H2 Database│
 *                                                    └─────────────┘
 *                                                            │
 *                                               Angular polls GET /api/orders
 *                                                            │
 *                                                    ┌───────▼──────┐
 *                                                    │  Angular UI  │ (live updates)
 *                                                    └──────────────┘
 */
@SpringBootApplication
@EnableScheduling
public class KafkaOrderApp {
    public static void main(String[] args) {
        SpringApplication.run(KafkaOrderApp.class, args);
    }
}
