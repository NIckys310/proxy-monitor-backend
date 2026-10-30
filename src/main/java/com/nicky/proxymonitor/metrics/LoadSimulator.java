package com.nicky.proxymonitor.metrics;

import com.nicky.proxymonitor.config.ServiceRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/** Genera llamadas aleatorias a través de los proxies para poblar los logs. */
@Component
public class LoadSimulator {

    static final int CALLS = 50;
    private static final String[] SKUS = {"LAPTOP-01", "MOUSE-02", "MONITOR-03", "TECLADO-04", "AUDIFONOS-05"};
    private static final String[] CUSTOMERS = {"ana", "luis", "maria", "carlos", "sofia"};

    private final ServiceRegistry registry;

    public LoadSimulator(ServiceRegistry registry) {
        this.registry = registry;
    }

    public Map<String, Object> run() {
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < CALLS; i++) {
                results.add(executor.submit(this::randomCall));
            }
        }
        long ok = results.stream().filter(LoadSimulator::succeeded).count();
        return Map.of("generated", CALLS, "success", ok, "errors", CALLS - ok);
    }

    private boolean randomCall() {
        var rnd = ThreadLocalRandom.current();
        String service = List.copyOf(registry.serviceIds()).get(rnd.nextInt(registry.serviceIds().size()));
        String sku = SKUS[rnd.nextInt(SKUS.length)];
        String operation;
        Map<String, Object> params;
        switch (service) {
            case "inventory" -> {
                operation = pick("check-stock", "reserve", "restock", "list");
                params = Map.of("sku", sku, "quantity", rnd.nextInt(1, 4));
            }
            case "orders" -> {
                operation = pick("create", "create", "create", "list");
                params = Map.of("sku", sku, "quantity", rnd.nextInt(1, 4),
                        "customer", CUSTOMERS[rnd.nextInt(CUSTOMERS.length)]);
            }
            default -> {
                operation = "process";
                params = Map.of("amount", Math.round(rnd.nextDouble(5, 1500) * 100) / 100.0,
                        "orderId", "ORD-" + rnd.nextInt(1001, 1030), "currency", "USD");
            }
        }
        try {
            registry.proxy(service).execute(operation, params);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String pick(String... options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    private static boolean succeeded(Future<Boolean> f) {
        try {
            return f.get();
        } catch (Exception e) {
            return false;
        }
    }
}
