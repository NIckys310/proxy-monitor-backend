package com.nicky.proxymonitor.web;

import com.nicky.proxymonitor.config.ServiceRegistry;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Punto de entrada del cliente: toda llamada pasa por el LoggingProxy del servicio. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRegistry registry;

    public ServiceController(ServiceRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/inventory/{operation}")
    public Map<String, Object> inventory(@PathVariable String operation,
                                         @RequestBody(required = false) Map<String, Object> params) {
        return call("inventory", operation, params);
    }

    @PostMapping("/orders/{operation}")
    public Map<String, Object> orders(@PathVariable String operation,
                                      @RequestBody(required = false) Map<String, Object> params) {
        return call("orders", operation, params);
    }

    @PostMapping("/payments/{operation}")
    public Map<String, Object> payments(@PathVariable String operation,
                                        @RequestBody(required = false) Map<String, Object> params) {
        return call("payments", operation, params);
    }

    private Map<String, Object> call(String serviceId, String operation, Map<String, Object> params) {
        Object result = registry.proxy(serviceId).execute(operation, params == null ? Map.of() : params);
        return Map.of("service", serviceId, "operation", operation, "result", result);
    }
}
