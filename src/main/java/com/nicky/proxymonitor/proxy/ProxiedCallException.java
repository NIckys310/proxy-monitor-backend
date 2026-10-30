package com.nicky.proxymonitor.proxy;

/** Error de un servicio real ya registrado por el proxy; conserva el requestId. */
public class ProxiedCallException extends RuntimeException {

    private final String requestId;

    public ProxiedCallException(String requestId, RuntimeException cause) {
        super(cause.getMessage(), cause);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
