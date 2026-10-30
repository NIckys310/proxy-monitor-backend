package com.nicky.proxymonitor.logging;

import java.time.Instant;

public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        LogStatus status,
        Instant timestamp,
        Object params,
        Object response,
        String errorMessage,
        String stackTrace) {
}
