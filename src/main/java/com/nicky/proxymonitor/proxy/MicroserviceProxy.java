package com.nicky.proxymonitor.proxy;

/**
 * Subject del patrón Proxy: interfaz común compartida por los servicios reales
 * y por el LoggingProxy. El cliente nunca sabe si habla con uno u otro.
 */
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);

    /** Identificador del microservicio (inventory, orders, payments). */
    String serviceId();
}
