package com.example.kafkaorder.service;

import com.example.kafkaorder.dto.OrderEvent;
import com.example.kafkaorder.dto.OrderRequest;
import com.example.kafkaorder.entity.Order;
import com.example.kafkaorder.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    /**
     * Place a new order:
     * 1. Save to H2 with PENDING status
     * 2. Publish Kafka event → consumer processes asynchronously
     *
     * The API returns immediately after step 2 — no waiting!
     * This is the power of Kafka: non-blocking, async processing.
     */
    @Transactional
    public Order placeOrder(OrderRequest request) {
        // Step 1: Persist order
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setProduct(request.getProduct());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setStatusMessage("Order received. Waiting for processing...");
        Order saved = orderRepository.save(order);

        log.info("💾 Order #{} saved to database | customer={} | product={}",
                saved.getId(), saved.getCustomerName(), saved.getProduct());

        // Step 2: Publish Kafka event
        OrderEvent event = OrderEvent.of(
                saved.getId(),
                saved.getCustomerName(),
                saved.getProduct(),
                saved.getQuantity(),
                saved.getPrice(),
                "PENDING",
                "Order placed, awaiting processing"
        );
        orderProducer.publishOrderPlaced(event);

        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    /**
     * Dashboard stats for the Angular UI header
     */
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", orderRepository.count());
        stats.put("pending", orderRepository.countByStatus(Order.OrderStatus.PENDING));
        stats.put("processing", orderRepository.countByStatus(Order.OrderStatus.PROCESSING));
        stats.put("completed", orderRepository.countByStatus(Order.OrderStatus.COMPLETED));
        stats.put("failed", orderRepository.countByStatus(Order.OrderStatus.FAILED));
        return stats;
    }
}
