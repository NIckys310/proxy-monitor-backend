package com.nicky.proxymonitor.proxy;

import com.nicky.proxymonitor.logging.LogEntry;
import com.nicky.proxymonitor.logging.LogStatus;
import com.nicky.proxymonitor.logging.LogStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingProxyTest {

    private final LogStore store = new LogStore(100);

    private final MicroserviceProxy<String> real = new MicroserviceProxy<>() {
        @Override
        public String execute(String operation, Object... params) {
            if (operation.equals("boom")) throw new IllegalStateException("fallo");
            return "ok:" + operation;
        }

        @Override
        public String serviceId() {
            return "fake";
        }
    };

    @Test
    void delegatesAndLogsSuccess() {
        MicroserviceProxy<String> proxy = new LoggingProxy<>(real, store);

        assertEquals("ok:ping", proxy.execute("ping", Map.of("a", 1)));

        LogEntry entry = store.all().getFirst();
        assertEquals(LogStatus.SUCCESS, entry.status());
        assertEquals("fake", entry.serviceId());
        assertEquals("ping", entry.operation());
        assertEquals("ok:ping", entry.response());
        assertNotNull(entry.requestId());
        assertNotNull(entry.timestamp());
    }

    @Test
    void logsErrorWithSummarizedStackTrace() {
        MicroserviceProxy<String> proxy = new LoggingProxy<>(real, store);

        ProxiedCallException ex = assertThrows(ProxiedCallException.class, () -> proxy.execute("boom"));

        LogEntry entry = store.all().getFirst();
        assertEquals(LogStatus.ERROR, entry.status());
        assertEquals(ex.getRequestId(), entry.requestId());
        assertTrue(entry.stackTrace().startsWith("IllegalStateException: fallo"));
        assertTrue(entry.stackTrace().lines().count() <= 6);
    }
}
