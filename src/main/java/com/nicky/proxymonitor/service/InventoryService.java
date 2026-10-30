package com.nicky.proxymonitor.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Servicio real de inventario. */
public class InventoryService extends AbstractMicroservice {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
            "LAPTOP-01", 25, "MOUSE-02", 140, "MONITOR-03", 40, "TECLADO-04", 75, "AUDIFONOS-05", 60));

    @Override
    public String serviceId() {
        return "inventory";
    }

    @Override
    protected Map<String, Function<Map<String, Object>, Object>> registerOperations() {
        Map<String, Function<Map<String, Object>, Object>> ops = new LinkedHashMap<>();
        ops.put("list", args -> Map.copyOf(stock));
        ops.put("check-stock", this::checkStock);
        ops.put("reserve", this::reserve);
        ops.put("restock", this::restock);
        return ops;
    }

    private Object checkStock(Map<String, Object> args) {
        String sku = required(args, "sku");
        return Map.of("sku", sku, "available", stockOf(sku));
    }

    private Object reserve(Map<String, Object> args) {
        String sku = required(args, "sku");
        int qty = intOf(args, "quantity", 1);
        int remaining = stock.compute(sku, (k, current) -> {
            if (current == null) throw new IllegalArgumentException("SKU inexistente: " + k);
            if (current < qty) throw new IllegalStateException("Stock insuficiente para " + k + ": " + current + " < " + qty);
            return current - qty;
        });
        return Map.of("sku", sku, "reserved", qty, "remaining", remaining);
    }

    private Object restock(Map<String, Object> args) {
        String sku = required(args, "sku");
        int qty = intOf(args, "quantity", 10);
        return Map.of("sku", sku, "available", stock.merge(sku, qty, Integer::sum));
    }

    private int stockOf(String sku) {
        Integer qty = stock.get(sku);
        if (qty == null) throw new IllegalArgumentException("SKU inexistente: " + sku);
        return qty;
    }
}
