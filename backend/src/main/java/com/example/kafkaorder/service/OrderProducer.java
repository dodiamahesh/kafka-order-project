package com.example.kafkaorder.service;

import com.example.kafkaorder.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║              KAFKA PRODUCER                              ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  Responsibility: Publish messages to Kafka topics        ║
 * ║                                                          ║
 * ║  Key Concepts:                                           ║
 * ║  • KafkaTemplate<K, V> — Spring's Kafka sender           ║
 * ║    K = key type (String)                                 ║
 * ║    V = value type (OrderEvent → serialized as JSON)      ║
 * ║                                                          ║
 * ║  • Message Key — used to route messages to partitions.   ║
 * ║    Same key → same partition → guaranteed ordering.      ║
 * ║    We use orderId as key so all events for one order     ║
 * ║    always go to the same partition (ordered delivery).   ║
 * ║                                                          ║
 * ║  • CompletableFuture — send() is non-blocking (async).   ║
 * ║    Callback fires when Kafka acknowledges the message.   ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${kafka.topic.order-placed}")
    private String orderPlacedTopic;

    @Value("${kafka.topic.order-processed}")
    private String orderProcessedTopic;

    /**
     *  આ method Kafka માં "order-placed" નામનો event મોકલે છે.
        જ્યારે order database માં save થાય છે, ત્યારે તરત જ આ method call થાય છે
        આ method order ની બધી માહિતી સાથે event publish કરે છે
        પછી Consumer (બીજી service) આ event ને receive કરે છે
        અને પછી તે background માં asynchronous રીતે process શરૂ કરે છે (એટલે કે main flow રોકાતો નથી)

     * Publish an "order-placed" event to Kafka.
     *
     * Called right after the order is saved to the database.
     * The Consumer will pick this up and begin processing asynchronously.
     *
     * @param event The order event containing all order details
     */
    public void publishOrderPlaced(OrderEvent event) {
        String messageKey = String.valueOf(event.getOrderId());

        // KafkaTemplate.send(topic, key, value)
        // key → determines which partition the message goes to
        // value → serialized to JSON by JsonSerializer
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(orderPlacedTopic, messageKey, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("❌ Failed to send order-placed event for orderId={}: {}",
                        event.getOrderId(), ex.getMessage());
            } else {
                // RecordMetadata tells you exactly where the message landed
                var metadata = result.getRecordMetadata();
                log.info("✅ Published order-placed | orderId={} | topic={} | partition={} | offset={}",
                        event.getOrderId(),
                        metadata.topic(),
                        metadata.partition(),     // Which partition (0, 1, or 2)
                        metadata.offset()         // Position within that partition
                );
            }
        });
    }

    /**
     * Publish an "order-processed" event after the consumer finishes.
     * Other services (notifications, analytics, etc.) can consume this.
     */
    public void publishOrderProcessed(OrderEvent event) {
        String messageKey = String.valueOf(event.getOrderId());

        kafkaTemplate.send(orderProcessedTopic, messageKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("❌ Failed to send order-processed event: {}", ex.getMessage());
                    } else {
                        log.info("✅ Published order-processed | orderId={} | status={}",
                                event.getOrderId(), event.getStatus());
                    }
                });
    }
}
