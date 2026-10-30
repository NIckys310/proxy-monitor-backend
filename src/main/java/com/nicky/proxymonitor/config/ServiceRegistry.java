package com.nicky.proxymonitor.config;

import com.nicky.proxymonitor.logging.LogStore;
import com.nicky.proxymonitor.proxy.LoggingProxy;
import com.nicky.proxymonitor.proxy.MicroserviceProxy;
import com.nicky.proxymonitor.service.AbstractMicroservice;
import com.nicky.proxymonitor.service.InventoryService;
import com.nicky.proxymonitor.service.OrderService;
import com.nicky.proxymonitor.service.PaymentService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Crea cada servicio real y lo envuelve en un LoggingProxy. El resto de la
 * aplicación solo recibe proxies, nunca el servicio real.
 */
@Component
public class ServiceRegistry {

    private final Map<String, MicroserviceProxy<Object>> proxies = new LinkedHashMap<>();
    private final Map<String, Set<String>> operations = new LinkedHashMap<>();

    public ServiceRegistry(LogStore logStore) {
        for (AbstractMicroservice real : List.of(new InventoryService(), new OrderService(), new PaymentService())) {
            proxies.put(real.serviceId(), new LoggingProxy<>(real, logStore));
            operations.put(real.serviceId(), real.operations());
        }
    }

    public MicroserviceProxy<Object> proxy(String serviceId) {
        MicroserviceProxy<Object> proxy = proxies.get(serviceId);
        if (proxy == null) throw new NoSuchElementException("Servicio inexistente: " + serviceId);
        return proxy;
    }

    public Set<String> serviceIds() {
        return proxies.keySet();
    }

    public Set<String> operationsOf(String serviceId) {
        return operations.get(serviceId);
    }
}
