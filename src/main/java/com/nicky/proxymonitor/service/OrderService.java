package com.nicky.proxymonitor.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Servicio real de pedidos. */
public class OrderService extends AbstractMicroservice {

    private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public String serviceId() {
        return "orders";
    }

    @Override
    protected Map<String, Function<Map<String, Object>, Object>> registerOperations() {
        Map<String, Function<Map<String, Object>, Object>> ops = new LinkedHashMap<>();
        ops.put("create", this::create);
        ops.put("get", args -> Map.copyOf(find(required(args, "orderId"))));
        ops.put("cancel", this::cancel);
        ops.put("list", args -> Map.of("count", orders.size(), "orders", orders.values().stream().map(Map::copyOf).toList()));
        return ops;
    }

    private Object create(Map<String, Object> args) {
        String id = "ORD-" + sequence.incrementAndGet();
        Map<String, Object> order = new ConcurrentHashMap<>();
        order.put("orderId", id);
        order.put("customer", str(args, "customer", "anonimo"));
        order.put("sku", required(args, "sku"));
        order.put("quantity", intOf(args, "quantity", 1));
        order.put("status", "CREATED");
        order.put("createdAt", Instant.now().toString());
        orders.put(id, order);
        return Map.copyOf(order);
    }

    private Object cancel(Map<String, Object> args) {
        Map<String, Object> order = find(required(args, "orderId"));
        if ("CANCELLED".equals(order.get("status"))) {
            throw new IllegalStateException("El pedido " + order.get("orderId") + " ya esta cancelado");
        }
        order.put("status", "CANCELLED");
        return Map.copyOf(order);
    }

    private Map<String, Object> find(String orderId) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) throw new IllegalArgumentException("Pedido inexistente: " + orderId);
        return order;
    }
}
