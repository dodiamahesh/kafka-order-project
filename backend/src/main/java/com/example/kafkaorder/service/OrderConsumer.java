package com.example.kafkaorder.service;

import com.example.kafkaorder.dto.OrderEvent;
import com.example.kafkaorder.entity.Order;
import com.example.kafkaorder.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║              KAFKA CONSUMER                              ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  Responsibility: Listen to Kafka topics and process      ║
 * ║  orders asynchronously.                                  ║
 * ║                                                          ║
 * ║  Key Concepts:                                           ║
 * ║  • @KafkaListener — marks a method as a Kafka consumer   ║
 * ║    topics    → which topic(s) to subscribe to            ║
 * ║    groupId   → consumer group name                       ║
 * ║                                                          ║
 * ║  • Consumer Group:                                       ║
 * ║    All consumers with the same groupId share the load.   ║
 * ║    If topic has 3 partitions → up to 3 consumers work    ║
 * ║    in parallel, each reading a different partition.      ║
 * ║                                                          ║
 * ║  • ConsumerRecord<K,V> — gives access to full metadata:  ║
 * ║    .topic()     → topic name                             ║
 * ║    .partition() → which partition                        ║
 * ║    .offset()    → message position                       ║
 * ║    .key()       → message key                            ║
 * ║    .value()     → deserialized payload (OrderEvent)      ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    /**
     * Method ને કોણ call કરે છે? Project માં calling ક્યાંય દેખાતું નથી.”
        ANSWER : 
        Kafka consumer method ને તમે manually call કરતા નથી.
        એ method ને Spring Kafka automatically call કરે છે જ્યારે Kafka topic માં message આવે.

        => Flow સમજીએ step by step
            - Order Service → Kafka માં message મોકલે (Producer)
            - Kafka topic → "order-placed"
            - Spring Kafka Consumer → એ topic ને listen કરે
            - Topic માં message આવે → Spring automatically method call કરે
            - તમારો method run થાય
========================================================================================= 
     * 
     * આ method "order-placed" નામના Kafka topic ને listen કરે છે અને દરેક order ને process કરે છે.

        Kafka માંથી messages (orders) એક પછી એક આવે છે (દરેક partition માટે)
        આ method તે message ને receive કરીને order નું processing કરે છે
        અંદરથી delay મૂકવામાં આવ્યો છે, જેથી real-life processing જેવી લાગણી આવે (simulation માટે)

     * Listen to the "order-placed" topic and process each order.
     *
     * Kafka delivers messages one at a time (per partition).
     * The method simulates real processing with a delay.
     */
    @KafkaListener(
            topics = "${kafka.topic.order-placed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeOrderPlaced(ConsumerRecord<String, OrderEvent> record) {

        OrderEvent event = record.value();

        log.info("📨 Received message | topic={} | partition={} | offset={} | orderId={}",
                record.topic(),
                record.partition(),
                record.offset(),
                event.getOrderId()
        );

        // ── Step 1: Mark order as PROCESSING ──
        updateOrderStatus(event.getOrderId(), Order.OrderStatus.PROCESSING,
                "Order received by processor. Working on it...");

        // ── Step 2: Simulate async processing (e.g., payment, inventory check) ──
        simulateProcessing();

        // ── Step 3: Randomly succeed or fail (80% success rate) ──
        boolean success = random.nextInt(10) < 8; // 80% success

        if (success) {
            updateOrderStatus(event.getOrderId(), Order.OrderStatus.COMPLETED,
                    "Order processed successfully! 🎉");

            // Publish a "completed" event — other services can consume this
            OrderEvent completedEvent = OrderEvent.of(
                    event.getOrderId(), event.getCustomerName(),
                    event.getProduct(), event.getQuantity(), event.getPrice(),
                    "COMPLETED", "Order fulfilled successfully"
            );
            orderProducer.publishOrderProcessed(completedEvent);

        } else {
            updateOrderStatus(event.getOrderId(), Order.OrderStatus.FAILED,
                    "Processing failed: Inventory check failed. Please retry.");

            OrderEvent failedEvent = OrderEvent.of(
                    event.getOrderId(), event.getCustomerName(),
                    event.getProduct(), event.getQuantity(), event.getPrice(),
                    "FAILED", "Inventory check failed"
            );
            orderProducer.publishOrderProcessed(failedEvent);
        }

        log.info("✅ Finished processing orderId={} | success={}", event.getOrderId(), success);
    }

    /**
     * Listen to the "order-processed" topic.
     * In a real app, this could trigger: email notifications, analytics, shipping, etc.
     * Here we just log it to show multi-consumer patterns.
     */
    @KafkaListener(
            topics = "${kafka.topic.order-processed}",
            groupId = "notification-group"   // ← Different group = separate consumer
    )
    public void consumeOrderProcessed(ConsumerRecord<String, OrderEvent> record) {
        OrderEvent event = record.value();
        log.info("🔔 Notification consumer: orderId={} finalStatus={} — would send email here",
                event.getOrderId(), event.getStatus());
    }

    // ── Private Helpers ──────────────────────────────────────

    private void updateOrderStatus(Long orderId, Order.OrderStatus status, String message) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            order.setStatusMessage(message);
            if (status == Order.OrderStatus.COMPLETED || status == Order.OrderStatus.FAILED) {
                order.setProcessedAt(LocalDateTime.now());
            }
            orderRepository.save(order);
            log.info("🔄 Order #{} status updated to {}", orderId, status);

            // Fire Spring event → SSE listener will push update to Angular
            eventPublisher.publishEvent(new OrderStatusChangedEvent(this, orderId, status.name()));
        });
    }

    /**
     * Simulates 2–5 seconds of real processing work.
     * In production: payment gateway call, inventory check, fraud detection, etc.
     */
    private void simulateProcessing() {
        try {
            int processingTime = 2000 + random.nextInt(3000); // 2–5 seconds
            log.info("⚙️  Processing order... ({}ms)", processingTime);
            TimeUnit.MILLISECONDS.sleep(processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
