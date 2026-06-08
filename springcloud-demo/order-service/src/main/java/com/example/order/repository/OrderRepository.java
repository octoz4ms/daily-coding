package com.example.order.repository;

import com.example.common.entity.Order;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class OrderRepository {

    private static final Map<Long, Order> ORDER_STORE = new LinkedHashMap<>();

    static {
        ORDER_STORE.put(1L, new Order(1L, "ORD001", 1L, "iPhone 15 Pro", 8999.0));
        ORDER_STORE.put(2L, new Order(2L, "ORD002", 2L, "MacBook Pro", 14999.0));
        ORDER_STORE.put(3L, new Order(3L, "ORD003", 1L, "AirPods Pro", 1899.0));
    }

    public Order findById(Long id) {
        return ORDER_STORE.get(id);
    }

    public Map<Long, Order> findAll() {
        return Map.copyOf(ORDER_STORE);
    }
}
