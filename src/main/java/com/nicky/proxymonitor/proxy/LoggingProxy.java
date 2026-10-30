package com.nicky.proxymonitor.proxy;

import com.nicky.proxymonitor.logging.LogEntry;
import com.nicky.proxymonitor.logging.LogStatus;
import com.nicky.proxymonitor.logging.LogStore;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Proxy de auditoría: envuelve cualquier MicroserviceProxy real y registra
 * inicio, duración, resultado y errores de cada llamada sin que el servicio real
 * sepa que está siendo observado.
 */
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final int STACK_TRACE_LINES = 5;

    private final MicroserviceProxy<T> target;
    private final LogStore logStore;

    public LoggingProxy(MicroserviceProxy<T> target, LogStore logStore) {
        this.target = target;
        this.logStore = logStore;
    }

    @Override
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        long start = System.nanoTime();
        logStore.recordStart(requestId, target.serviceId(), operation, timestamp);
        try {
            T result = target.execute(operation, params);
            logStore.save(new LogEntry(requestId, target.serviceId(), operation, elapsedMs(start),
                    LogStatus.SUCCESS, timestamp, paramsOf(params), result, null, null));
            return result;
        } catch (RuntimeException ex) {
            logStore.save(new LogEntry(requestId, target.serviceId(), operation, elapsedMs(start),
                    LogStatus.ERROR, timestamp, paramsOf(params), null, ex.getMessage(), summarize(ex)));
            throw new ProxiedCallException(requestId, ex);
        }
    }

    @Override
    public String serviceId() {
        return target.serviceId();
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static Object paramsOf(Object[] params) {
        return params != null && params.length == 1 ? params[0] : params;
    }

    private static String summarize(Throwable ex) {
        String head = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        String frames = Arrays.stream(ex.getStackTrace())
                .limit(STACK_TRACE_LINES)
                .map(f -> "  at " + f)
                .collect(Collectors.joining("\n"));
        return head + "\n" + frames;
    }
}
