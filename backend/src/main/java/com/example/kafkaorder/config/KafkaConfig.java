package com.example.kafkaorder.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Kafka Topic Configuration
 *
 * KEY CONCEPTS:
 * ─────────────
 * Topic      → A named channel in Kafka (like a table in a DB)
 * Partition  → A topic is split into partitions for parallelism
 *              (e.g., 3 partitions = 3 consumer threads can read in parallel)
 * Replicas   → Copies of partitions for fault tolerance
 *              (1 replica = no redundancy, fine for local dev)
 *
 * In production: use more partitions + replicas!
 */
@Configuration
public class KafkaConfig implements WebMvcConfigurer {

    @Value("${kafka.topic.order-placed}")
    private String orderPlacedTopic;

    @Value("${kafka.topic.order-processed}")
    private String orderProcessedTopic;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Topic: order-placed
     * Published when: user places an order via the API
     * Consumed by: OrderConsumer
     * Partitions: 3 (3 orders can be processed in parallel)
     */
    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(orderPlacedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic: order-processed
     * Published when: consumer finishes processing an order
     * Partitions: 3
     * (This topic could be consumed by a notification service, analytics, etc.)
     */
    @Bean
    public NewTopic orderProcessedTopic() {
        return TopicBuilder.name(orderProcessedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * CORS config — allows Angular (localhost:4200) to call our API
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
