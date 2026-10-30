package com.nicky.proxymonitor.metrics;

import com.nicky.proxymonitor.config.ServiceRegistry;
import com.nicky.proxymonitor.logging.LogEntry;
import com.nicky.proxymonitor.logging.LogStatus;
import com.nicky.proxymonitor.logging.LogStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private static final int MAX_PAGE_SIZE = 200;

    private final LogStore logStore;
    private final ServiceRegistry registry;

    public MetricsService(LogStore logStore, ServiceRegistry registry) {
        this.logStore = logStore;
        this.registry = registry;
    }

    public Map<String, Object> summary() {
        List<LogEntry> all = logStore.all();
        List<Map<String, Object>> services = new ArrayList<>();
        for (String serviceId : registry.serviceIds()) {
            services.add(stats(serviceId, all.stream().filter(e -> e.serviceId().equals(serviceId)).toList()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("services", services);
        body.put("global", stats("all", all));
        body.put("generatedAt", Instant.now().toString());
        return body;
    }

    public PageResult<LogEntry> logs(String service, LogStatus status, Instant from, Instant to, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page debe ser >= 0");
        if (size < 1 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        List<LogEntry> filtered = logStore.all().stream()
                .filter(e -> service == null || service.isBlank() || e.serviceId().equals(service))
                .filter(e -> status == null || e.status() == status)
                .filter(e -> from == null || !e.timestamp().isBefore(from))
                .filter(e -> to == null || !e.timestamp().isAfter(to))
                .sorted(Comparator.comparing(LogEntry::timestamp).reversed())
                .toList();
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        int totalPages = (int) Math.ceil(filtered.size() / (double) size);
        return new PageResult<>(filtered.subList(start, end), page, size, filtered.size(), totalPages);
    }

    private static Map<String, Object> stats(String serviceId, List<LogEntry> entries) {
        long total = entries.size();
        long errors = entries.stream().filter(e -> e.status() == LogStatus.ERROR).count();
        double avg = entries.stream().mapToLong(LogEntry::durationMs).average().orElse(0);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("serviceId", serviceId);
        s.put("totalCalls", total);
        s.put("successCount", total - errors);
        s.put("errorCount", errors);
        s.put("errorRate", total == 0 ? 0.0 : round(errors * 100.0 / total));
        s.put("successRate", total == 0 ? 0.0 : round((total - errors) * 100.0 / total));
        s.put("avgResponseMs", round(avg));
        return s;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
