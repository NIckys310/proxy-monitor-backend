package com.nicky.proxymonitor.web;

import com.nicky.proxymonitor.logging.LogStatus;
import com.nicky.proxymonitor.metrics.LoadSimulator;
import com.nicky.proxymonitor.metrics.MetricsService;
import com.nicky.proxymonitor.metrics.PageResult;
import com.nicky.proxymonitor.logging.LogEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metrics;
    private final LoadSimulator simulator;

    public MetricsController(MetricsService metrics, LoadSimulator simulator) {
        this.metrics = metrics;
        this.simulator = simulator;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return metrics.summary();
    }

    @GetMapping("/logs")
    public PageResult<LogEntry> logs(@RequestParam(required = false) String service,
                                     @RequestParam(required = false) LogStatus status,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return metrics.logs(service, status, from, to, page, size);
    }

    @PostMapping("/simulate-load")
    public Map<String, Object> simulateLoad() {
        return simulator.run();
    }
}
