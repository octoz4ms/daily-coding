package com.example.order.repository;

import com.example.common.entity.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class OrderRepository {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    private static final Map<Long, Order> STORE = new ConcurrentHashMap<>();

    static {
        Order order1 = new Order(1L, "ORD20250609001", 1L, "MacBook Pro", 15999.00);
        Order order2 = new Order(2L, "ORD20250609002", 2L, "AirPods Pro", 1899.00);
        Order order3 = new Order(3L, "ORD20250609003", 3L, "iPhone 15", 7999.00);
        STORE.put(order1.getId(), order1);
        STORE.put(order2.getId(), order2);
        STORE.put(order3.getId(), order3);
        ID_GENERATOR.set(4L);
    }

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(ID_GENERATOR.getAndIncrement());
        }
        if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            order.setOrderNo("ORD" + System.currentTimeMillis());
        }
        STORE.put(order.getId(), order);
        return order;
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(STORE.get(id));
    }

    public Optional<Order> findByOrderNo(String orderNo) {
        return STORE.values().stream()
                .filter(o -> o.getOrderNo().equals(orderNo))
                .findFirst();
    }
}
