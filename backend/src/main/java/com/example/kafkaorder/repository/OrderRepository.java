package com.example.kafkaorder.repository;

import com.example.kafkaorder.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find all orders sorted newest first
    List<Order> findAllByOrderByCreatedAtDesc();

    // Find orders by status (used for monitoring)
    List<Order> findByStatus(Order.OrderStatus status);

    // Count orders per status
    long countByStatus(Order.OrderStatus status);
}
