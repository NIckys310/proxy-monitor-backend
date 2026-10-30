package com.nicky.proxymonitor.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/** Servicio real de pagos. El 10% de las llamadas falla intencionalmente. */
public class PaymentService extends AbstractMicroservice {

    static final double FAILURE_RATE = 0.10;

    private final Map<String, Map<String, Object>> payments = new ConcurrentHashMap<>();

    @Override
    public String serviceId() {
        return "payments";
    }

    @Override
    public Object execute(String operation, Object... params) {
        if (ThreadLocalRandom.current().nextDouble() < FAILURE_RATE) {
            simulateLatency();
            throw new IllegalStateException("Fallo simulado: pasarela de pagos no disponible (timeout)");
        }
        return super.execute(operation, params);
    }

    @Override
    protected Map<String, Function<Map<String, Object>, Object>> registerOperations() {
        Map<String, Function<Map<String, Object>, Object>> ops = new LinkedHashMap<>();
        ops.put("process", this::process);
        ops.put("refund", this::refund);
        ops.put("status", args -> Map.copyOf(find(required(args, "paymentId"))));
        return ops;
    }

    private Object process(Map<String, Object> args) {
        double amount = doubleOf(args, "amount", 0);
        if (amount <= 0) throw new IllegalArgumentException("El monto debe ser mayor a 0");
        String id = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> payment = new ConcurrentHashMap<>();
        payment.put("paymentId", id);
        payment.put("orderId", str(args, "orderId", "N/A"));
        payment.put("amount", amount);
        payment.put("currency", str(args, "currency", "USD"));
        payment.put("status", "APPROVED");
        payment.put("processedAt", Instant.now().toString());
        payments.put(id, payment);
        return Map.copyOf(payment);
    }

    private Object refund(Map<String, Object> args) {
        Map<String, Object> payment = find(required(args, "paymentId"));
        payment.put("status", "REFUNDED");
        return Map.copyOf(payment);
    }

    private Map<String, Object> find(String paymentId) {
        Map<String, Object> payment = payments.get(paymentId);
        if (payment == null) throw new IllegalArgumentException("Pago inexistente: " + paymentId);
        return payment;
    }
}
