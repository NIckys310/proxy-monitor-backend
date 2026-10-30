# Proxy Monitor · Backend

Sistema de monitoreo de microservicios con **Proxy de auditoría y logging** (Spring Boot 4, Java 21).

## Patrón Proxy

| Rol del patrón | Clase |
|---|---|
| Subject (interfaz común) | `proxy/MicroserviceProxy<T>` — `T execute(String operation, Object... params)` |
| RealSubject | `service/InventoryService`, `service/OrderService`, `service/PaymentService` |
| Proxy | `proxy/LoggingProxy<T>` — envuelve cualquier servicio |
| Cliente | `web/ServiceController`, `metrics/LoadSimulator` |

`config/ServiceRegistry` crea cada servicio real y lo envuelve en un `LoggingProxy`; el resto de la app solo conoce proxies.

El `LoggingProxy`:
1. Registra el **inicio** de la llamada (requestId UUID + timestamp).
2. Delega en el servicio real.
3. Registra **duración y resultado** (`SUCCESS`) o el **error con stack trace resumido** (`ERROR`, 5 frames).

Cada log incluye `serviceId`, `operation`, `durationMs`, `status`, `requestId`, `timestamp` (+ parámetros y respuesta).

`PaymentService` falla intencionalmente en el **10%** de las llamadas.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/services/inventory/{operation}` | `list`, `check-stock`, `reserve`, `restock` |
| POST | `/api/services/orders/{operation}` | `create`, `get`, `cancel`, `list` |
| POST | `/api/services/payments/{operation}` | `process`, `refund`, `status` |
| GET | `/api/metrics/summary` | total de llamadas, tasa de error y tiempo promedio por servicio |
| GET | `/api/metrics/logs?service=&status=&from=&to=&page=&size=` | logs filtrados y paginados (`from`/`to` en ISO-8601) |
| POST | `/api/metrics/simulate-load` | genera 50 llamadas aleatorias |

Los parámetros de cada operación van en el body JSON, por ejemplo:

```bash
curl -X POST localhost:8080/api/services/payments/process -H "Content-Type: application/json" -d '{"amount": 120.5, "orderId": "ORD-1001"}'
```

## Ejecutar

```bash
./mvnw spring-boot:run
```

Variables de entorno: `PORT` (default 8080), `CORS_ORIGINS` (default `*`, lista separada por comas).

## Despliegue

Railway con el `Dockerfile` incluido.
