package com.nicky.proxymonitor.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Servicios reales sin latencia simulada para que los tests sean rápidos. */
class ServicesTest {

    static class FastInventory extends InventoryService {
        @Override
        protected void simulateLatency() {
        }
    }

    static class FastOrders extends OrderService {
        @Override
        protected void simulateLatency() {
        }
    }

    static class FastPayments extends PaymentService {
        @Override
        protected void simulateLatency() {
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void inventoryReservesAndRestocks() {
        var inventory = new FastInventory();

        var reserved = (Map<String, Object>) inventory.execute("reserve", Map.of("sku", "LAPTOP-01", "quantity", 5));
        assertEquals(20, reserved.get("remaining"));

        var restocked = (Map<String, Object>) inventory.execute("restock", Map.of("sku", "LAPTOP-01", "quantity", 10));
        assertEquals(30, restocked.get("available"));

        var stock = (Map<String, Object>) inventory.execute("check-stock", Map.of("sku", "LAPTOP-01"));
        assertEquals(30, stock.get("available"));
    }

    @Test
    void inventoryRejectsInvalidInput() {
        var inventory = new FastInventory();

        assertThrows(IllegalArgumentException.class, () -> inventory.execute("check-stock", Map.of("sku", "NO-EXISTE")));
        assertThrows(IllegalArgumentException.class, () -> inventory.execute("check-stock", Map.of()));
        assertThrows(IllegalStateException.class,
                () -> inventory.execute("reserve", Map.of("sku", "LAPTOP-01", "quantity", 9999)));
        assertThrows(UnsupportedOperationException.class, () -> inventory.execute("borrar-todo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ordersLifecycle() {
        var orders = new FastOrders();

        var created = (Map<String, Object>) orders.execute("create", Map.of("sku", "MOUSE-02", "quantity", 2, "customer", "ana"));
        String id = (String) created.get("orderId");
        assertEquals("CREATED", created.get("status"));

        var fetched = (Map<String, Object>) orders.execute("get", Map.of("orderId", id));
        assertEquals("ana", fetched.get("customer"));

        var cancelled = (Map<String, Object>) orders.execute("cancel", Map.of("orderId", id));
        assertEquals("CANCELLED", cancelled.get("status"));

        assertThrows(IllegalStateException.class, () -> orders.execute("cancel", Map.of("orderId", id)));
        assertThrows(IllegalArgumentException.class, () -> orders.execute("get", Map.of("orderId", "ORD-0")));
    }

    @Test
    void paymentsFailAroundTenPercent() {
        var payments = new FastPayments();
        int calls = 5000;
        int failures = 0;
        for (int i = 0; i < calls; i++) {
            try {
                payments.execute("process", Map.of("amount", 10));
            } catch (IllegalStateException e) {
                failures++;
            }
        }
        double rate = failures / (double) calls;
        assertTrue(rate > 0.08 && rate < 0.12, "tasa de fallo fuera de rango: " + rate);
    }

    @Test
    void paymentsRejectInvalidAmount() {
        var payments = new FastPayments();
        // Cada llamada puede caer en la falla aleatoria (IllegalStateException); el resto debe validar el monto.
        for (int i = 0; i < 50; i++) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> payments.execute("process", Map.of("amount", -5)));
            assertTrue(ex instanceof IllegalArgumentException || ex.getMessage().startsWith("Fallo simulado"));
        }
    }
}
