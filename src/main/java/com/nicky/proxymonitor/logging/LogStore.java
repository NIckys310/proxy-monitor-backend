package com.nicky.proxymonitor.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/** Almacén en memoria de logs, el más reciente primero, con tamaño máximo. */
@Component
public class LogStore {

    private static final Logger log = LoggerFactory.getLogger(LogStore.class);

    private final Deque<LogEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger();
    private final int maxEntries;

    public LogStore(@Value("${app.logs.max-entries:5000}") int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public void recordStart(String requestId, String serviceId, String operation, Instant timestamp) {
        log.info("[START] requestId={} service={} operation={} at={}", requestId, serviceId, operation, timestamp);
    }

    public void save(LogEntry entry) {
        log.info("[END] requestId={} service={} operation={} status={} durationMs={}",
                entry.requestId(), entry.serviceId(), entry.operation(), entry.status(), entry.durationMs());
        entries.addFirst(entry);
        if (size.incrementAndGet() > maxEntries && entries.pollLast() != null) {
            size.decrementAndGet();
        }
    }

    public List<LogEntry> all() {
        return new ArrayList<>(entries);
    }
}
