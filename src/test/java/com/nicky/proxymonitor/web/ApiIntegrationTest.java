package com.nicky.proxymonitor.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void inventoryCallGoesThroughProxyAndIsLogged() throws Exception {
        mvc.perform(post("/api/services/inventory/check-stock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"MOUSE-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.available", is(140)));

        mvc.perform(get("/api/metrics/logs").param("service", "inventory"))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].operation", is("check-stock")))
                .andExpect(jsonPath("$.content[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.content[0].requestId", notNullValue()))
                .andExpect(jsonPath("$.content[0].timestamp", notNullValue()))
                .andExpect(jsonPath("$.content[0].durationMs", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.content[0].params.sku", is("MOUSE-02")));
    }

    @Test
    void ordersEndpointCreatesOrder() throws Exception {
        mvc.perform(post("/api/services/orders/create")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"LAPTOP-01\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status", is("CREATED")));
    }

    @Test
    void paymentsEndpointProcessesOrFailsWithRequestId() throws Exception {
        var result = mvc.perform(post("/api/services/payments/process")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50}"))
                .andReturn().getResponse();
        if (result.getStatus() == 200) {
            mvc.perform(get("/api/metrics/logs").param("service", "payments"))
                    .andExpect(jsonPath("$.content[0].status", is("SUCCESS")));
        } else {
            mvc.perform(get("/api/metrics/logs").param("service", "payments"))
                    .andExpect(jsonPath("$.content[0].status", is("ERROR")))
                    .andExpect(jsonPath("$.content[0].stackTrace", notNullValue()));
        }
    }

    @Test
    void unknownOperationReturns400AndLogsError() throws Exception {
        mvc.perform(post("/api/services/orders/borrar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestId", notNullValue()));

        mvc.perform(get("/api/metrics/logs").param("status", "ERROR"))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].errorMessage", notNullValue()));
    }

    @Test
    void simulateLoadGenerates50CallsAndSummaryReflectsThem() throws Exception {
        mvc.perform(post("/api/metrics/simulate-load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(50)));

        mvc.perform(get("/api/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services", hasSize(3)))
                .andExpect(jsonPath("$.global.totalCalls", is(50)))
                .andExpect(jsonPath("$.services[0].errorRate", notNullValue()))
                .andExpect(jsonPath("$.services[0].avgResponseMs", notNullValue()));

        mvc.perform(get("/api/metrics/logs").param("size", "10").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalPages", is(5)));
    }

    @Test
    void dateRangeFilterUsesIsoInstants() throws Exception {
        mvc.perform(post("/api/services/inventory/list")).andExpect(status().isOk());

        mvc.perform(get("/api/metrics/logs").param("from", "2000-01-01T00:00:00Z").param("to", "2000-01-02T00:00:00Z"))
                .andExpect(jsonPath("$.totalElements", is(0)));
        mvc.perform(get("/api/metrics/logs").param("from", "2000-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void invalidPageSizeReturns400() throws Exception {
        mvc.perform(get("/api/metrics/logs").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }
}
