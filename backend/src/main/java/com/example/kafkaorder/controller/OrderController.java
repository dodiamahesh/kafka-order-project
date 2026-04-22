package com.example.kafkaorder.controller;

import com.example.kafkaorder.dto.OrderRequest;
import com.example.kafkaorder.entity.Order;
import com.example.kafkaorder.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * REST Controller — exposes the Order API to Angular.
 *
 * Endpoints:
 *  POST   /api/orders          → Place a new order
 *  GET    /api/orders          → List all orders
 *  GET    /api/orders/{id}     → Get a single order
 *  GET    /api/orders/stats    → Dashboard stats
 *  GET    /api/orders/stream   → SSE stream for real-time updates
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // SSE emitters list — all connected Angular clients
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * POST /api/orders
     * Places an order and immediately returns 201 Created.
     * Kafka handles processing in the background.
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody OrderRequest request) {
        log.info("📥 POST /api/orders — customer={}", request.getCustomerName());
        Order order = orderService.placeOrder(request);
        notifyClients(); // Push SSE update to Angular
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * GET /api/orders
     * Returns all orders sorted newest first.
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * GET /api/orders/{id}
     * Returns a single order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * GET /api/orders/stats
     * Returns count per status for the dashboard header.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }

    /**
     * GET /api/orders/stream
     *
     * Server-Sent Events (SSE) endpoint.
     * Angular subscribes once; server pushes updates whenever an order
     * status changes (instead of polling every N seconds).
     *
     * How it works:
     * 1. Angular opens an EventSource connection to /api/orders/stream
     * 2. Spring keeps the HTTP connection open (SseEmitter)
     * 3. When Kafka consumer updates an order, notifyClients() fires
     * 4. All connected Angular clients receive the latest orders
     */
    @GetMapping("/stream")
    public SseEmitter streamOrders() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send current state immediately on connect
        try {
            emitter.send(SseEmitter.event()
                    .name("orders")
                    .data(orderService.getAllOrders()));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        log.info("📡 SSE client connected | total clients={}", emitters.size());
        return emitter;
    }

    /**
     * Called by the consumer after updating order status.
     * Pushes live updates to all connected Angular clients.
     */
    public void notifyClients() {
        List<Order> orders = orderService.getAllOrders();
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("orders")
                        .data(orders));
                return false;
            } catch (IOException e) {
                return true; // Remove disconnected clients
            }
        });
    }
}
