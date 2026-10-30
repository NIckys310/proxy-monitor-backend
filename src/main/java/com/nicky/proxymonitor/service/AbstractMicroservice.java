package com.nicky.proxymonitor.service;

import com.nicky.proxymonitor.proxy.MicroserviceProxy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/** Base de los servicios reales: despacha la operación y simula latencia de red. */
public abstract class AbstractMicroservice implements MicroserviceProxy<Object> {

    private final Map<String, Function<Map<String, Object>, Object>> operations;

    protected AbstractMicroservice() {
        this.operations = registerOperations();
    }

    protected abstract Map<String, Function<Map<String, Object>, Object>> registerOperations();

    public Set<String> operations() {
        return operations.keySet();
    }

    @Override
    public Object execute(String operation, Object... params) {
        var handler = operations.get(operation);
        if (handler == null) {
            throw new UnsupportedOperationException(
                    "Operacion '" + operation + "' no soportada por " + serviceId() + ". Disponibles: " + operations.keySet());
        }
        simulateLatency();
        return handler.apply(argsOf(params));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> argsOf(Object[] params) {
        if (params != null && params.length > 0 && params[0] instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    protected static void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(20, 250));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected static String str(Map<String, Object> args, String key, String fallback) {
        Object v = args.get(key);
        return v == null ? fallback : v.toString();
    }

    protected static String required(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null || v.toString().isBlank()) {
            throw new IllegalArgumentException("Parametro requerido: " + key);
        }
        return v.toString();
    }

    protected static int intOf(Map<String, Object> args, String key, int fallback) {
        Object v = args.get(key);
        if (v == null) return fallback;
        try {
            return v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parametro '" + key + "' debe ser numerico");
        }
    }

    protected static double doubleOf(Map<String, Object> args, String key, double fallback) {
        Object v = args.get(key);
        if (v == null) return fallback;
        try {
            return v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parametro '" + key + "' debe ser numerico");
        }
    }
}
