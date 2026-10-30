package com.nicky.proxymonitor.metrics;

import com.nicky.proxymonitor.config.ServiceRegistry;
import com.nicky.proxymonitor.logging.LogEntry;
import com.nicky.proxymonitor.logging.LogStatus;
import com.nicky.proxymonitor.logging.LogStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private LogStore store;
    private MetricsService metrics;

    @BeforeEach
    void setUp() {
        store = new LogStore(100);
        metrics = new MetricsService(store, new ServiceRegistry(store));
        save("inventory", LogStatus.SUCCESS, 100, 0);
        save("inventory", LogStatus.SUCCESS, 200, 1);
        save("payments", LogStatus.ERROR, 50, 2);
        save("payments", LogStatus.SUCCESS, 150, 3);
        save("orders", LogStatus.SUCCESS, 120, 4);
    }

    private void save(String service, LogStatus status, long ms, int minute) {
        store.save(new LogEntry(UUID.randomUUID().toString(), service, "op", ms, status,
                T0.plusSeconds(minute * 60L), Map.of(), null, null, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void summaryPerService() {
        var services = (List<Map<String, Object>>) metrics.summary().get("services");
        var payments = services.stream().filter(s -> s.get("serviceId").equals("payments")).findFirst().orElseThrow();
        var inventory = services.stream().filter(s -> s.get("serviceId").equals("inventory")).findFirst().orElseThrow();

        assertEquals(2L, payments.get("totalCalls"));
        assertEquals(50.0, payments.get("errorRate"));
        assertEquals(100.0, payments.get("avgResponseMs"));
        assertEquals(0.0, inventory.get("errorRate"));
        assertEquals(150.0, inventory.get("avgResponseMs"));
    }

    @Test
    void filtersByServiceStatusAndDates() {
        assertEquals(2, metrics.logs("inventory", null, null, null, 0, 20).totalElements());
        assertEquals(1, metrics.logs(null, LogStatus.ERROR, null, null, 0, 20).totalElements());
        assertEquals(3, metrics.logs(null, null, T0.plusSeconds(60), T0.plusSeconds(180), 0, 20).totalElements());
        assertEquals(1, metrics.logs("payments", LogStatus.SUCCESS, T0.plusSeconds(180), null, 0, 20).totalElements());
    }

    @Test
    void paginatesNewestFirst() {
        var first = metrics.logs(null, null, null, null, 0, 2);
        var last = metrics.logs(null, null, null, null, 2, 2);

        assertEquals(5, first.totalElements());
        assertEquals(3, first.totalPages());
        assertEquals("orders", first.content().getFirst().serviceId());
        assertEquals(1, last.content().size());
        assertTrue(metrics.logs(null, null, null, null, 9, 2).content().isEmpty());
    }

    @Test
    void rejectsInvalidPaging() {
        assertThrows(IllegalArgumentException.class, () -> metrics.logs(null, null, null, null, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> metrics.logs(null, null, null, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> metrics.logs(null, null, null, null, 0, 500));
    }

    @Test
    void logStoreKeepsMaxEntries() {
        var small = new LogStore(3);
        for (int i = 0; i < 10; i++) {
            small.save(new LogEntry("id-" + i, "x", "op", 1, LogStatus.SUCCESS, T0, null, null, null, null));
        }
        assertEquals(3, small.all().size());
        assertEquals("id-9", small.all().getFirst().requestId());
    }
}
